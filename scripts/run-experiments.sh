#!/bin/bash

cliopts="$@"	# optional parameters
max_procs=1	# parallelism
timeout=1080	# timeout in seconds

DATA="../data/base_scholar/acm_SVM"
THETA="0.5"
LAMBDA="0.1 " #0.00001"


#INDEX="INV ALLPAIRS L2AP L2"
#INDEX="L2"
INDEX="L2"
echo results/{1/.}_t{2}_l{3}_MB-{4}

parallel --timeout ${timeout} --ungroup --max-procs ${max_procs}  "./streaming {1} -t {2} -l {3} -i {4} ${cliopts} -f SVMLIB > results/{1/.}_t{2}_l{3}_MB-{4}"  ::: $DATA ::: $THETA ::: $LAMBDA ::: $INDEX

#echo parallel --timeout ${timeout} --ungroup --max-procs ${max_procs}  "./minibatch {1} -t {2} -l {3} -i {4} ${cliopts} -f SVMLIB > results/{1/.}_t{2}_l{3}_MB-{4}"  ::: $DATA ::: $THETA ::: $LAMBDA ::: $INDEX


# #INDEX="INV L2AP L2"
# INDEX="INV L2AP L2"
# parallel --timeout ${timeout} --ungroup --max-procs ${max_procs} "scripts/streaming {1} -t {2} -l {3} -i {4} ${cliopts} > results/{1/.}_t{2}_l{3}_STR-{4}" ::: $DATA ::: $THETA ::: $LAMBDA ::: $INDEX
