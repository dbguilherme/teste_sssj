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
import re

for file in sys.argv[1:]:
  ofile = open(file)
  c=0;
  for line in ofile:
    
    if not line:
        continue;
    vet=line.split(" ")
    flag=False
    for i in ((vet)):
        
        d= re.findall('(\d{4})',i)
        for x in d:
            # Then it found a match!
            
            if((int(x))<1900 or  (int(x)> 2100)):
                   continue;
            
            print  (x+" " + str(c)) 
            c+=1
            flag=True
    if(flag==False):
        print (line +" ")