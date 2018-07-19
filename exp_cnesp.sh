MODEL=$1
SUBPATH=$2
DATASET=$3
RP=$4
L2=0.1
OUTPUTFILE="nc_"$SUBPATH"_"$DATASET".log"

nohup java -Xmx32g -Xms8g -jar ns.jar  -modelname $MODEL -thread 20 -subpath $SUBPATH -outputscope true -num_iter 1000 -neural continuous0 -emb polyglot -embsize 64 -reg $L2 -lr 0.01 -optimizer lbfgs -dataset $DATASET -lang cn -discrete true -discardintest false -bac $RP -syntax true -useperl false -outputsem2012 false -unipos true > $OUTPUTFILE 2>&1

