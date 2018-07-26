# Negation Scope

This page contains the code used in the work "Learning with Structured Representations for Negation Scope Extraction" published at ACL 2018.

## Contents
1. [Usage](#usage)
2. [SourceCode](#sourcecode)
3. [Citation](#citation)


## Usage

Prerequisite: JRE (1.8 or later)

Run the following command to try out the Linear, Semi and Latent models described in the paper.

>>> java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMN6 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true

>>> java -Xmx32g -Xms8g -jar ns.jar  -modelname SEMIBOI2 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true

>>> java -Xmx32g -Xms8g -jar ns.jar  -modelname OIBAMNLatent6 -thread 4 -subpath default -outputscope true -num_iter 1000 -reg 0.1 -optimizer lbfgs -dataset cdsco -lang en -discrete true -discardintest false -syntax true -useperl true -outputsem2012 true -unipos true -latentmax 2

Or you can simply run

>>> exp_cdsco.sh

Please download the Bioscope and CNeSp dataset before trying out the additional experiments. Use the following scripts for running such additional experiments:
>>> exp_bioscope.sh
>>> exp_cnesp.sh


## SourceCode

The source code is written in Java, which can be found under the "src" folder.

Note that this is a Maven project. Therefore you can import this project as a Maven project in eclipse.


## Citation
If you use our code, please cite our work:
@InProceedings{P18-2085,
  author = 	"Li, Hao
		and Lu, Wei",
  title = 	"Learning with Structured Representations for Negation Scope Extraction",
  booktitle = 	"Proceedings of the 56th Annual Meeting of the Association for Computational Linguistics (Volume 2: Short Papers)",
  year = 	"2018",
  publisher = 	"Association for Computational Linguistics",
  pages = 	"533--539",
  location = 	"Melbourne, Australia",
  url = 	"http://aclweb.org/anthology/P18-2085"
}