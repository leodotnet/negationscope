1. Extract the data and place the sub-folder into directory "data" with the same path as ns.jar.
2. run the following command to perform Linear, Semi and Latent models described in the paper.

java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMN6 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true
java -Xmx32g -Xms8g -jar ns.jar  -modelname SEMIBOI2 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true
java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMNLatent6 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true -latentmax 2

