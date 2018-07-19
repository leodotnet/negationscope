####Run the code######
Run the following command to perform Linear, Semi and Latent models described in the paper.

java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMN6 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true
java -Xmx32g -Xms8g -jar ns.jar  -modelname SEMIBOI2 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true
java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMNLatent6 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true -latentmax 2

Or you can just run
exp_cdsco.sh

Notice that please download Bioscope and CNeSp dataset before trying out the additional experiments.
The following two scripts are the command lines to run negationscope model on two additional datasets.
exp_bioscope.sh
exp_cnesp.sh


####Source code######

The source code is written in Java in "src" folder.

Note that this is a Maven project so that you can import this project as a Maven project in eclipse.