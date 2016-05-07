<!DOCTYPE html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>StaTweetstics</title>

    <link rel="stylesheet" href="css/normalize.css">
    <link rel="stylesheet" href="css/bootstrap.min.css">
    <link rel="stylesheet" href="css/bootstrap-switch.min.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
    <link rel="stylesheet" href="css/vex.css">
    <link rel="stylesheet" href="css/vex-theme-wireframe.css">
    <link rel="stylesheet" href="css/main.css">

    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
</head>

<body>
    <div id="particles"></div>
    
    <div id="tweetbox">
        <div id="tweetWrite">
            <input type="text" id="tweetInput" placeholder="Your tweet here!" autocomplete="off">

            <a id="tweetButton" onmousedown="changeURL(this)">
                <div>
                    <i class="fa fa-twitter fa-2x"></i>
                </div>
            </a>
        </div>
    </div>
    
    <div class="container-fluid" id="content">
        <div class="row-fluid">
            <div class="col col-sm-10" id="suggestions">
                <h1>StaTweetstically speaking, <br>it'd be cool to tweet about...</h1>
                
                <ul id="topsugslist"></ul>
                <p id="topsugsslide"></p>
                
                <div id="usernameDisplay">
                    <p>suggestions for</p>
                    <h3>@</h3>
                    <input type="text" id="usernameInput" placeholder="username" autocomplete="off" readonly="true">
                    <i class="fa fa-pencil" id="usernameEdit"></i>
                </div>
                
                <input type="checkbox" name="my-checkbox" checked data-on-color="success" data-off-color="success" data-handle-width="31" data-on-text="slide" data-off-text="list">
                
                <br>
                <div id="sortby" class="radiogroup">
                    <ul>
                        <li>
                            <input type='radio' name='sortby' value='retweets' id='retweets' checked="checked"/>
                            <label for='retweets'><div>Retweets</div></label>
                        </li>
                        <li>
                            <input type='radio' name='sortby' value='likes' id='likes'/>
                            <label for='likes'><div>Likes</div></label>
                        </li>
                    </ul>
                </div>
                
                <br>
                <div id="sugsgroup" class="radiogroup">
                    <ul>
                        <li>
                            <input type='trend' name='trendtype' value='yourtrending' id='yourtrending' checked="checked"/>
                            <label for='yourtrending'><div>Your Trending</div></label>
                        </li>
                        <li>
                            <input type='radio' name='trendtype' value='twittertrending' id='twittertrending'/>
                            <label for='twittertrending'><div>Twitter Trending</div></label>
                        </li>
                    </ul>
                </div>
            </div><!--
            
            --><div class="col col-sm-5" id="tweetStats">
                <p id="closeStats">x</p>
                <h3 id="statsTitle">#springBreak Trend Graph:</h3>
                <div id="div-chart"></div>
            </div>
        </div>
    </div>

    <script src="js/jquery-2.2.2.min.js"></script>
    <script src="js/bootstrap.min.js"></script>
    <script src="js/bootstrap-switch.min.js"></script>
    <script src="js/jquery.particleground.min.js"></script>
    <script src="js/vex.combined.min.js"></script>
    <script>vex.defaultOptions.className = 'vex-theme-wireframe';</script>
    <script src="js/main.js"></script>
</body>
</html>