from __future__ import print_function
import sys
import gzip
import re
import nltk
import time
import dateutil.parser
from collections import Counter
#from bs4 import BeautifulSoup
from nltk.corpus import stopwords
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.feature_extraction.text import HashingVectorizer
from sklearn.feature_extraction.text import CountVectorizer

def clean_text(text):
  str= text.split(";",1);
  #print(str[1]);
  if (len(str)<1):
      return
  #print(str)    
  words = str[1] # remove weird characters
  
  
#  words = BeautifulSoup(words.lower()).get_text() # remove HTML
  words = re.sub("[^a-z^0-9]", " ", words)  # keep letters only
  words = words.split() # tokenization
  words = [w for w in words if not w in stops] # remove stopwords
 # print(words)
  return( " ".join(words)) # return single string

def output_batch(corpus, timestamps, tscounts, vectorizer):
  # ensure timestamps are unique
  newts = []
  #print("teste")
  for ts in timestamps:
    newts.append(ts * 1 + tscounts[ts]) # convert to ms
    tscounts[ts] += 1 # add 1 ms of delay to each identical timestamp
  timestamps = newts # ignore collisions (no collision unless there are >1000 pages with identical timestamp), check the output after!

  features = vectorizer.fit_transform(corpus)
#  print(features)
  
  dataset = zip(timestamps, features)
#  print("Dataset statistics: {} x {} sparse matrix with {} non-zero elements".format(features.shape[0], features.shape[1], features.nnz), file=sys.stderr)
  

  for (ts, vec) in dataset:
    vec.sort_indices()
   # print (vec)
    out =  str(ts)+ " " + " ".join( [":".join([str(k),str(v)]) for k,v in zip(vec.indices, vec.data) ])
    print(out)
   # print("temp")      

tscounts = Counter()
stops = set(stopwords.words("english"))
# feature extraction
vectorizer = TfidfVectorizer(analyzer="word", max_features=10000, min_df=1)
#vectorizer = HashingVectorizer(analyzer="word", non_negative=True, norm="l2") # n_features=2**20
vectorizer=CountVectorizer(min_df=1)
corpus = []
timestamps = []
ts=0
for file in sys.argv[1:]:
  gfile = open(file)
  for line in gfile:
    if not line:
        continue;
    
    if line.startswith('T'):
      # process date
      datestring = line.lstrip('T\t')
      date = dateutil.parser.parse(datestring) # datestring to datetime
      ts = long(time.mktime(date.timetuple())) # datetime to Unix timestamp
      timestamps.append(ts)
    else:
      # process tweet
      ts =ts+1  
      timestamps.append(ts)
      
      clean_tweet = (clean_text(line))
    #  print (clean_tweet)
      corpus.append(clean_tweet)
      #if len(corpus) >= 10:
output_batch(corpus, timestamps, tscounts, vectorizer)
        #print(vectorizer.vocabulary_)
corpus = []
timestamps = []

# remember to sort tweets and check for duplicates in postprocessing!	
