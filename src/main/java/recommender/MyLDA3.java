package edu.brown.cs.suggest;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.io.IOException;
import edu.brown.cs.suggest.ORM.*;
/**
--------------
NEED TO CHANGE
--------------
*/
public class MyLDA3 {
	public double alpha; // Hyper-parameter alpha
	public double beta; // Hyper-parameter beta
	public double betaBackground; // Hyper-parameter backgroundbeta
	public int numTopics; // Number of topics
	public int numIterations; // Number of Gibbs sampling iterations
	public int topWords; // Number of most probable words for each topic
	public double[] gamma;
	

	public double alphaSum; // alpha * numTopics
	public double betaSum; // beta * vocabularySize
	public double betaBackgroundSum; //background beta

	public List<List<List<Integer>>> corpus; // Word ID-based corpus
	//z[u][d]
	public List<List<Integer>> topicAssignments; // Topics assignments for words
													// in the corpus
	public int numDocuments; // Number of documents in the corpus
	public int numWordsInCorpus; // Number of words in the corpus
	public int numUsers;

	public HashMap<Word, Integer> word2IdVocabulary; // Vocabulary to get ID
														// given a word
	public HashMap<Integer, Word> id2WordVocabulary; // Vocabulary to get word
														// given an ID
	public HashMap<Integer, Tweet> id2Doc;
	public List<User> users;
	public int vocabularySize; // The number of word types in the corpus
	// numDocs * vocabularySize matrix marks if a word is in the foreground or background
	public int[][][] foreBack;
	// Given a document: number of its words assigned to the indexed topic
	//C_ua[u][a]
	//docTopicCount[u][d][a]
	public int[][] docTopicCount;
	// numTopics * vocabularySize matrix
	// Given a topic: number of times a word type assigned to the topic
	public int[][] topicWordCount;
	// Total number of words assigned to a topic
	public int[] sumTopicWordCount;
	//the background words
	public int[] backCount;
	//forward and bacground count
	public long[] fbCount;
	public Random random = new Random();

	public double[][] theta_general;
	public float[][] phi_word;
	public double[] phi_background;
	public double[] rho;

	// Double array used to sample a topic
	public double[] multiPros;
	public int savestep = 0;
	public MyLDA3(double inAlpha, double inBeta , double inBetaB, double inGamma0,double inGamma1,  int inNumTopics, 
		int inNumIterations, int inTopWords, int inSaveStep, List<User> _users) {
		users = _users;
		numUsers = users.size();
		alpha = inAlpha;
		beta = inBeta;
		betaBackground = inBetaB;
		numTopics = inNumTopics;
		numIterations = inNumIterations;
		topWords = inTopWords;
		savestep = inSaveStep;
		gamma = new double[2];
		gamma[0] = inGamma0;
		gamma[1] = inGamma1;
		word2IdVocabulary = new HashMap<>();
		id2WordVocabulary = new HashMap<>();
		id2Doc = new HashMap<>();
		corpus = new ArrayList<>();
		numDocuments = 0;
		numWordsInCorpus = 0;
		int indexWord = -1;
		int indexDoc = 0;
		int indexUser = 0;
		for (User u : users) {
			List<List<Integer>> userDocs = new ArrayList<>();
			for (Tweet twt : u.getTweets()) {
				List<Integer> docWords = new ArrayList<>();
				for (Word word : twt.words()) {
					if (word2IdVocabulary.containsKey(word)) {
						docWords.add(word2IdVocabulary.get(word));
					} else {
						indexWord += 1;
						word2IdVocabulary.put(word, indexWord);
						id2WordVocabulary.put(indexWord,word);
						docWords.add(indexWord);
					}
				}
				if (docWords.size() != 0) {
					id2Doc.put(indexDoc,twt);
					userDocs.add(docWords);
					indexDoc++;
					numDocuments++;
					numWordsInCorpus += docWords.size();
				}
				
			}
			corpus.add(userDocs);
			indexUser++;
		}

		vocabularySize = word2IdVocabulary.size();
		docTopicCount = new int[numUsers][numTopics];
		topicWordCount = new int[numTopics][vocabularySize];
		sumTopicWordCount = new int[numTopics];
		fbCount = new long[2];
		rho = new double[2];
		//foreBack = new int[num][numDocuments][vocabularySize];
		backCount = new int[vocabularySize];
		multiPros = new double[numTopics];
		phi_background = new double[vocabularySize];
		phi_word = new float[numTopics][vocabularySize];
		theta_general = new double[numUsers][numTopics];
		
		for (int i = 0; i < numTopics; i++) {
			multiPros[i] = 1.0 / numTopics;
		}
		alphaSum = numTopics * alpha;
		betaSum = vocabularySize * beta;
		betaBackgroundSum = betaBackground * vocabularySize;
		System.out.println("Corpus size: " + numDocuments + " docs, "+ numWordsInCorpus + " words");
		System.out.println("Vocabuary size: " + vocabularySize);
		System.out.println("Number of topics: " + numTopics);
		System.out.println("alpha: " + alpha);
		System.out.println("beta: " + beta);
		System.out.println("Number of sampling iterations: " + numIterations);
		System.out.println("Number of top topical words: " + topWords);
		initialize();
	}
	public void initialize() {
		System.out.println("Randomly initializing topic assignments...");

		topicAssignments = new ArrayList<List<Integer>>();
		// for (int tIndex = 0; tIndex < numTopics; tIndex++)
		// 	for (int dIndex = 0; dIndex < numDocuments; dIndex++)
		// 		docTopicCount[dIndex][tIndex] = 0;
		foreBack = new int[numUsers][][];

		//docTopicCount = new int[numUsers][][];
		for (int u = 0;  u < numUsers; u++) {
			int userTwts = corpus.get(u).size();
			foreBack[u] = new int[userTwts][];
			List<Integer> userAssignments = new ArrayList<>();
			for (int d = 0; d < userTwts; d++) {
				//List<Integer> topics = new ArrayList<Integer>();
				int docSize =  corpus.get(u).get(d).size();
				int topic = nextDiscrete(multiPros);
				foreBack[u][d] = new int[docSize];
				docTopicCount[u][topic] += 1;
				for (int w = 0; w < docSize; w++) {
					//foreBack[i][j] = 0;
					double r = random.nextDouble();
					if (r > 0.5) {
						fbCount[1]+=1;
						topicWordCount[topic][corpus.get(u).get(d).get(w)] += 1;
						sumTopicWordCount[topic] += 1;
						foreBack[u][d][w] = 1;
					} else {
						fbCount[0]+=1;
						backCount[corpus.get(u).get(d).get(w)] += 1;
						foreBack[u][d][w] = 0;
					}
					
				}
				userAssignments.add(topic);
			}
			topicAssignments.add(userAssignments);
		}
		System.out.println("Initialization Complete");
	}
	public void inference() {
		System.out.println("Running Gibbs sampling inference: ");
		for (int iter = 1; iter <= numIterations; iter++) {
			//System.out.println("\tSampling iteration: " + (iter));
			// System.out.println("\t\tPerplexity: " + computePerplexity());
			sample();

			if ((savestep > 0) && (iter % savestep == 0) && (iter < numIterations)) {
				System.out.println("\t\tSaving the output from the " + iter + "^{th} sample");
				//expName = orgExpName + "-" + iter;
				printTopicAssignmentRankWords();
			}
		}
		System.out.println("Sampling completed!");
		System.out.println("Finalizing Distribution");
		finalizeDistribution();
	}
	public void sample() {
		for (int uIndex = 0; uIndex < numUsers; uIndex++) {
			int docSize = corpus.get(uIndex).size();
			for (int dIndex = 0; dIndex < docSize; dIndex++) {
				int wordSize = corpus.get(uIndex).get(dIndex).size();
				sampleDoc(uIndex, dIndex);
				for (int wIndex = 0; wIndex < wordSize; wIndex++) { 
					sampleWord(uIndex, dIndex, wIndex);
				}
			}
		}
	}
	public void sampleDoc(int uIndex,int dIndex) {
		int docSize = corpus.get(uIndex).get(dIndex).size();
		// topicAssignments = z[u][d]
		int topic = topicAssignments.get(uIndex).get(dIndex);
		int foreWordCount = 0;
		//C_ua
		assert docTopicCount[uIndex][topic] >= 0 : "docTopicCount not greater than zero";
		assert docSize >= 0: "docSize  not greater than zero";
		docTopicCount[uIndex][topic] -= 1;
		for (int tIndex = 0; tIndex < numTopics; tIndex++) {
			//P_topic
			multiPros[tIndex] = ((docTopicCount[uIndex][tIndex] + alpha) / (docSize + alphaSum));
		}
		for (int wIndex = 0; wIndex < docSize; wIndex++) {
				if (foreBack[uIndex][dIndex][wIndex] == 1) {
					// Get current word
					int word = corpus.get(uIndex).get(dIndex).get(wIndex);
					// Decrease counts
					//C_word: 
					topicWordCount[topic][word] -= 1;
					//countAllWords:
					sumTopicWordCount[topic] -= 1;
					for (int tIndex = 0; tIndex < numTopics; tIndex++) {
						multiPros[tIndex] *= Math.pow(((topicWordCount[tIndex][word] + beta ) / (sumTopicWordCount[tIndex] + betaBackgroundSum )), (double) 1);
					}

			}
		}
		double randz = Math.random();

		double sum = 0;

		for (int a = 0; a < numTopics; a++) {
			sum += multiPros[a];
		}

		double thred = 0;

		int chosena = -1;

		for (int a = 0; a < numTopics; a++) {
			thred += multiPros[a] / sum;
			if (thred >= randz) {
				chosena = a;
				break;
			}
		}
		if (chosena == -1) {
			System.out.println("chosena equals -1, error!");
		}
		topic = chosena;//nextDiscrete(multiPros);
		topicAssignments.get(uIndex).set(dIndex, topic);
		docTopicCount[uIndex][topic] += 1;

		for (int wIndex = 0; wIndex < docSize; wIndex++) {
			if (foreBack[uIndex][dIndex][wIndex] == 1) {
				int word = corpus.get(uIndex).get(dIndex).get(wIndex);
				topicWordCount[topic][word] += 1;
				sumTopicWordCount[topic] += 1;
			}
		}
	}
	public void sampleWord(int uIndex, int dIndex, int wIndex) {
		double [] fbProbs = new double[2];
		double foreProbs;
		double backProbs;
		int fb = foreBack[uIndex][dIndex][wIndex];
		int word = corpus.get(uIndex).get(dIndex).get(wIndex);
		int topic = topicAssignments.get(uIndex).get(dIndex);
		//C_lv
		fbCount[fb]--;
		if (fb == 1) {
			topicWordCount[topic][word] -= 1;
			sumTopicWordCount[topic] -= 1;
		} else {
			//C_b
			backCount[word] -=1;
		}
		double denom = fbCount[0] + fbCount[1] + gamma[0] + gamma[1];
		fbProbs[0] = (fbCount[0] + gamma[0])/denom;
		fbProbs[1] = (fbCount[1] + gamma[1])/denom;
		fbProbs[0] *= (backCount[word] + betaBackground)/(fbCount[0] + betaBackgroundSum);
		fbProbs[1] *= (topicWordCount[topic][word] + beta)/(sumTopicWordCount[topic] + betaBackgroundSum);
		fb = nextDiscrete(fbProbs);

		fbCount[fb]++;
		if (fb == 1) {
			topicWordCount[topic][word] += 1;
			sumTopicWordCount[topic] += 1;
		} else {
			//C_b
			backCount[word] +=1;
		}

	}
	private void finalizeDistribution() {
		for (int u = 0; u < numUsers; u++) {
			int c_u_a = 0;
			for (int t = 0; t < numTopics; t++) {
				c_u_a += docTopicCount[u][t];
			}
			for (int a = 0; a < numTopics; a++) {
				theta_general[u][a] = (docTopicCount[u][a] + alpha)/(c_u_a + alphaSum);
			}
		}

		for (int a = 0; a < numTopics; a++) {
			int c_v = 0;
			for (int v = 0; v < vocabularySize; v++) {
				c_v += topicWordCount[a][v];
			}
			for (int v = 0; v < vocabularySize; v++) {
				phi_word[a][v] =Double.valueOf( (topicWordCount[a][v] + beta)
						/ (c_v + betaSum)).floatValue();
			}
		}
		int c_b_v = 0;
		for (int v = 0; v < vocabularySize; v++) {
			c_b_v += backCount[v];
		}
		for (int v = 0; v < vocabularySize; v++) {
			phi_background[v] = (backCount[v] + betaBackground)
					/ (c_b_v + betaBackgroundSum);
		}
		rho[0] = 2*(fbCount[0] + gamma[0])/(fbCount[0] + fbCount[1] + gamma[0] + gamma[1]);
		rho[1] = 2*(fbCount[1] + gamma[1])/(fbCount[0] + fbCount[1] + gamma[0] + gamma[1]);
	}
	private int nextDiscrete(double[] probs) {
		double sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
		}

		double r = random.nextDouble() * sum;

		sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r)
				return i;
		}
		return probs.length - 1;
	}
		
	 private void printTopicAssignmentRankWords() {
		for (int tIndex = 0; tIndex < numTopics; tIndex++) {
			System.out.println("Topic" + tIndex + ":");

			Map<Integer, Integer> wordCount = new TreeMap<Integer, Integer>();
			for (int wIndex = 0; wIndex < vocabularySize; wIndex++) {
				wordCount.put(wIndex, topicWordCount[tIndex][wIndex]);
			}
			wordCount = sortByValueDescending(wordCount);

			Set<Integer> mostLikelyWords = wordCount.keySet();
			int count = 0;
			for (Integer index : mostLikelyWords) {
				if (count < topWords) {
					double pro = (topicWordCount[tIndex][index] + beta) / (sumTopicWordCount[tIndex] + betaSum);
					pro = Math.round(pro * 1000000.0) / 1000000.0;
					System.out.println(" " + id2WordVocabulary.get(index) + "(" + pro + ")");
					count += 1;
				} else {
					System.out.print("\n\n");
					break;
				}
			}
		}
	}
	public void print() {
		printTopicAssignmentRankWords();
	}

	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDescending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>(){
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
			{
					int compare = (o1.getValue()).compareTo(o2.getValue());
					return -compare;
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
				result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	public static List<Integer> getTop(double[] array, int i) {
		int index = 0;
		List<Integer> rankList = new ArrayList<Integer>();
		HashSet<Integer> scanned = new HashSet<Integer>();
		double max = Double.MIN_VALUE;
		for (int m = 0; m < i && m < array.length; m++) {
			max = Double.MIN_VALUE;
			for (int no = 0; no < array.length; no++) {
				if (!scanned.contains(no)) {
					if (array[no] > max) {
						index = no;
						max = array[no];
					} else if (array[no] == max && Math.random() > 0.5) {
						index = no;
						max = array[no];
					}
				}
			}
			if (!scanned.contains(index)) {
				scanned.add(index);
				rankList.add(index);
			}
			//System.out.println(m + "\t" + index);
		}
		return rankList;
	}
	public static List<Integer> getTop(float[] array, int i) {
		int index = 0;
		List<Integer> rankList = new ArrayList<Integer>();
		HashSet<Integer> scanned = new HashSet<Integer>();
		double max = Double.MIN_VALUE;
		for (int m = 0; m < i && m < array.length; m++) {
			max = Double.MIN_VALUE;
			for (int no = 0; no < array.length; no++) {
				if (!scanned.contains(no)) {
					if (array[no] > max) {
						index = no;
						max = array[no];
					} else if (array[no] == max && Math.random() > 0.5) {
						index = no;
						max = array[no];
					}
				}
			}
			if (!scanned.contains(index)) {
				scanned.add(index);
				rankList.add(index);
			}
			//System.out.println(m + "\t" + index);
		}
		return rankList;
	}
	public List<List<List<Tweet>>> getTopicsToRank() {
		List<List<List<Tweet>>> finalResult = new ArrayList<>();
		for (int uIndex = 0; uIndex < numUsers; uIndex++ ) {
			List<List<Tweet>> result = new ArrayList<>();
			List<Integer> topics = getTop(theta_general[uIndex],numTopics+1);
			//System.out.println("toppics: "+topics);
			for (Integer tIndex : topics) {
				//System.out.println("tppks: "+tIndex);
				//int tIndex = topicIndex.intValue();
				Word.clearCache();
				List<Tweet> tweetSub = new ArrayList<>();
				for (int dIndex = 0; dIndex < corpus.get(uIndex).size(); dIndex++) {
					if (topicAssignments.get(uIndex).get(dIndex) == tIndex) {
						//System.out.println("topic: "+topicAssignments.get(uIndex).get(dIndex)+", tIndex: "+tIndex);
						int docSize = corpus.get(uIndex).get(dIndex).size();
						//System.out.println("-  dIndex: "+dIndex);
						Tweet twt = id2Doc.get(Integer.valueOf(dIndex));
						assert twt != null : "tweet is null";
						tweetSub.add(new TweetProxy(twt.getId()));
					}
				}
				result.add(tweetSub);
			}
			finalResult.add(result);
			//System.out.println("u0t2"+finalResult.get(0).get(2));
			
		}
		return finalResult;
	}
	public void printFB() {
		for (int uIndex = 0; uIndex < numUsers; uIndex++ ) {
			System.out.println("User"+uIndex+" handle: "+users.get(uIndex).getHandle()+":");
			//for (int tIndex = 0; tIndex < numTopics; tIndex++) {
			List<Integer> topics = getTop(theta_general[uIndex],numTopics+1);
			for (Integer tIndex : topics) {
				System.out.println("Topic"+tIndex+" ("+theta_general[uIndex][tIndex]+"):");
				List<Integer> rankList = getTop(phi_word[tIndex],100000);
				int i = 0;
				for (Integer rnk : rankList) {
					if (i >= topWords) {
						continue;
					}
					i++;
					System.out.println("Word:{"+id2WordVocabulary.get(rnk)+" , "+phi_word[tIndex][rnk]+"}");
				}
				//for (int dIndex = 0; dIndex < corpus.get(uIndex).size(); dIndex++) {

					// if (topicAssignments.get(uIndex).get(dIndex) == tIndex) {
					// 	Tweet twt = id2Doc.get(dIndex);
					// 	List<String> foreg = new ArrayList<>();
					// 	//List<String> backg = new ArrayList<>();
					// 	int docSize = corpus.get(uIndex).get(dIndex).size();
					// 	for (int wIndex = 0; wIndex < docSize; wIndex++) {
					// 		if (foreBack[uIndex][dIndex][wIndex] == 1) {
					// 			// Get current word
					// 			int word = corpus.get(uIndex).get(dIndex).get(wIndex);
					// 			Word w = id2WordVocabulary.get(word);
					// 			foreg.add(w.toString());
					// 		}
					// 	}
					// 	System.out.print("  ");
					// 	System.out.println("Tweet:");
					// 	System.out.print("  ");
					// 	System.out.println(" - text: "+twt.text());
					// 	System.out.print("  ");
					// 	System.out.println(" - words: "+twt.words());
					// 	System.out.print("  ");
					// 	System.out.println(" - foreW: "+foreg);
					// }
				}
			}
		}
		public void printFB(int topTopics) {
		for (int uIndex = 0; uIndex < numUsers; uIndex++ ) {
			System.out.println("User"+uIndex+" handle: "+users.get(uIndex).getHandle()+":");
			//for (int tIndex = 0; tIndex < numTopics; tIndex++) {
			int t = 0;
			List<Integer> topics = getTop(theta_general[uIndex],numTopics+1);
			for (Integer tIndex : topics) {
				if (t > topTopics) {
					break;
				}
				t++;
				System.out.println("Topic"+tIndex+" ("+theta_general[uIndex][tIndex]+"):");
				List<Integer> rankList = getTop(phi_word[tIndex],100000);
				int i = 0;
				for (Integer rnk : rankList) {
					if (i >= topWords) {
						continue;
					}
					i++;
					System.out.println("Word:{"+id2WordVocabulary.get(rnk)+" , "+phi_word[tIndex][rnk]+"}");
				}
				}
			}
		}
}


















