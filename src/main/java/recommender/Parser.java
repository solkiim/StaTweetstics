package edu.brown.cs.suggest;
import java.util.Arrays;
import com.google.common.base.Splitter;
import com.google.common.base.CharMatcher;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import java.io.File;
import java.util.LinkedHashMap;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

public interface Parser<T,S> {
	T parse(S raw);
	default List<T> parse(List<S> raw) {
		List<T> res = new ArrayList<T>();
		for (S r: raw) {
			res.add(parse(r));
		}
		return res;
	}
}