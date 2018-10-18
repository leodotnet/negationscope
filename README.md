# Negation Scope

This page contains the code used in the work "Learning with Structured Representations for Negation Scope Extraction" published at ACL 2018.

## Contents
1. [Usage](#usage)
2. [SourceCode](#sourcecode)
3. [Citation](#citation)


## Usage

Prerequisite: JRE (1.8 or later)

Run the following command to try out the Linear, Semi and Latent models described in the paper.
```sh
java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMN6 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true
```
```sh
java -Xmx32g -Xms8g -jar ns.jar  -modelname SEMIBOI2 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true
```
```sh
java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMNLatent6 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true -latentmax 2
```


Or you can simply run

```sh
./exp_cdsco.sh
```

Please download the Bioscope and CNeSp dataset before trying out the additional experiments. Use the following scripts for running such additional experiments:

```sh
./exp_bioscope.sh
```
```sh
./exp_cnesp.sh
```


## SourceCode

The source code is written in Java, which can be found under the "src" folder.

Note that it is a Maven project, therefore we recommend you to import this project as a Maven project in eclipse.


## Citation
If you use our code, please cite our work:
@inproceedings{li2017learning,
  title={Learning Latent Sentiment Scopes for Entity-Level Sentiment Analysis.},
  author={Li, Hao and Lu, Wei},
  booktitle={AAAI},
  pages={3482--3489},
  year={2017}
}



Email to hao_li@mymail.sutd.edu.sg if any inquery.
