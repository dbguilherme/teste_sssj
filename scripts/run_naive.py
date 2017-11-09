#!/bin/bash

limiar=0.1
for i in `seq 1 9`;
        do
                v=$(echo "$i/10"|bc -l)
                echo $v
                java -Xmx2G -jar /tmp/TESTE.jar -t $v  -l 0.0000001 -i   INV -f SVMLIB  /home/guilherme/git/teste_sssj/data/base_scholar/acm_SVM
                
                
        done    




