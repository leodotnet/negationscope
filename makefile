D='20171215_0900'

build:
	ant

test:
	echo $D"  ..."

movelog:
	#mkdir logs
	mv *.log ./logs/.

main:
	#MODEL=$1, SUBPATH=$2, NEURAL=$3, DATASET=$4, RP=$5, SYNTAX=$6
	#by default, it has rp contraints
	 ./exp_cdsco.sh OI2 $D"_OI2" none cdsco false true
	 ./exp_cdsco.sh OIN4 $D"_OIN4" none cdsco false true
	 ./exp_cdsco.sh OIBAM4 $D"_OIBAM4" none cdsco false true
	 ./exp_cdsco.sh OIBAMN6 $D"_OIBAMN6" none cdsco false true
	 ./exp_cdsco.sh OIBAMN6 $D"_OIBAMN6_rp" none cdsco true true
	 ./exp_cdsco.sh OIBAMN6 $D"_OIBAMN6_p" none cdsco2 false true
	 ./exp_cdsco.sh OIBAMN6 $D"_OIBAMN6_p_e" continuous0 cdsco2 false true
	 ./exp_cdsco.sh OIBAMN6 $D"_OIBAMN6_p_e_rp" continuous0 cdsco2 true true
	 ./exp_cdsco.sh OIBAMN6 $D"_OIBAMN6_p_e_NOs" continuous0 cdsco2 false false

	 cp experiments/negation/models/OIBAMN6/en/$D"_OIBAMN6_p_e"/OIBAMN6.model models/discrete/. 	 


#sythetic:
	 ./exp_cdsco.sh OIBAMN6 $D"_OIBAMN6_p_e_sythetic" continuous0 simple_wiki2 false true
	 ./exp_cdsco.sh OIBAMN6 $D"_OIBAMN6_p_e_sythetic_rp" continuous0 simple_wiki2 true true



#MODEL=$1, SUBPATH=$2, DATASET=$3, RP=$4,emb=$5

bioscope_e:
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_e" bioscope_full false continuous0
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_e" bioscope_clinic false continuous0
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_e" bioscope_abstracts false continuous0
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_e_rp" bioscope_full true continuous0
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_e_rp" bioscope_clinic true continuous0
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_e_rp" bioscope_abstracts true continuous0

bioscope:
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6" bioscope_full false none
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6" bioscope_clinic false none
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6" bioscope_abstracts false none
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_rp" bioscope_full true none
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_rp" bioscope_clinic true none
	./exp_bioscope.sh OIBAMN6 $D"_OIBAMN6_rp" bioscope_abstracts true none


#MODEL=$1, SUBPATH=$2 ,DATASET=$3, RP=$4
cnesp:
	./exp_cnesp.sh OIBAMN6 $D"_OIBAMN6_e_cnesp" cnesp_product false
	./exp_cnesp.sh OIBAMN6 $D"_OIBAMN6_e_cnesp" cnesp_financial false
	./exp_cnesp.sh OIBAMN6 $D"_OIBAMN6_e_cnesp" cnesp_scientific false
	./exp_cnesp.sh OIBAMN6 $D"_OIBAMN6_e_cnesp_rp" cnesp_product true
	./exp_cnesp.sh OIBAMN6 $D"_OIBAMN6_e_cnesp_rp" cnesp_financial true
	./exp_cnesp.sh OIBAMN6 $D"_OIBAMN6_e_cnesp_rp" cnesp_scientific true

	








