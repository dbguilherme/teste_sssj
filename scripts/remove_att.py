#!/usr/bin/env python

str="../data/dirty_1000_100"
file = open(str, "r")
fileout = open(str+"B", "w") 
for line in file: 
    array=line.split(",")
    for i in range(len(array)-5):
        print (line.split(",")[i]) 
        fileout.write(line.split(",")[i]+", ");
    fileout.write(line.split(",")[i+1]+"\n ");
    
fileout.close();
file.close();