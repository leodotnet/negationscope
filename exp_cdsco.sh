java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMN6 -thread 20 -subpath OIBAMN6 -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true
java -Xmx32g -Xms8g -jar ns.jar  -modelname SEMIBOI2 -thread 20 -subpath OIBAMN6 -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true
java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMNLatent6 -thread 20 -subpath OIBAMN6 -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true -latent 2

