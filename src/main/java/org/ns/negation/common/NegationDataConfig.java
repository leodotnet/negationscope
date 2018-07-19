package org.ns.negation.common;

import java.util.HashMap;

public class NegationDataConfig {

	public static String DATA = "data";

	public static String PRETRAIN_MODEL_PATH = "models";

	public static String UNIPOS_MAP_PATH = "data//uni_pos_map//en.txt";

	public static final HashMap<String, String> dataset2Path = new HashMap<String, String>() {
		{
			put("cdsco_en", "cdsco");
			put("cdsco2_en", "cdsco2");

			put("simple_wiki_en", "simple_wiki");
			put("simple_wiki2_en", "simple_wiki2");
			
			
			put("cdsco-end2end_en", "cdsco-end2end");
			put("cdsco2-end2end_en", "cdsco2-end2end");

			/*
			 * put("simple_wiki_simple_en", "simple_wiki");
			 * put("simple_wiki_prefixal_en", "simple_wiki");
			 * put("simple_wiki_suffixal_en", "simple_wiki");
			 * put("simple_wiki_multiword_en", "simple_wiki");
			 * put("simple_wiki_lexical_en", "simple_wiki");
			 * put("simple_wiki_genre_en", "simple_wiki");
			 * put("simple_wiki_unseen_en", "simple_wiki");
			 * 
			 * put("simple_wiki_simple2_en", "simple_wiki2");
			 * put("simple_wiki_prefixal2_en", "simple_wiki2");
			 * put("simple_wiki_suffixal2_en", "simple_wiki2");
			 * put("simple_wiki_multiword2_en", "simple_wiki2");
			 * put("simple_wiki_lexical2_en", "simple_wiki2");
			 * put("simple_wiki_genre2_en", "simple_wiki2");
			 * put("simple_wiki_unseen2_en", "simple_wiki2");
			 */
			put("simple_wiki_suffixal_en", "simple_wiki");
			put("cdsco_suffixal_en", "cdsco");
			
			put("simple_wiki_suffixal2_en", "simple_wiki2");
			put("cdsco_suffixal2_en", "cdsco2");

			put("simple_wiki_unseen2_en", "simple_wiki2");
			put("cdsco_unseen2_en", "cdsco2");

			put("bioscope_abstracts_en", "bioscope");
			put("bioscope_full_en", "bioscope");
			put("bioscope_clinic_en", "bioscope");
			
			put("bioscope_abstracts-end2end_en", "bioscope-end2end");
			put("bioscope_full-end2end_en", "bioscope-end2end");
			put("bioscope_clinic-end2end_en", "bioscope-end2end");

			put("cnesp_financial_cn", "CNeSp");
			put("cnesp_product_cn", "CNeSp");
			put("cnesp_scientific_cn", "CNeSp");
			
			put("cnesp_product-end2end_cn", "CNeSp-end2end");

			put("cnesp_product_all_cn", "CNeSp");
		}
	};

	public static HashMap<String, String[]> dataset2Files = new HashMap<String, String[]>() {
		{
			put("cdsco_en", new String[] { "SEM-2012-SharedTask-CD-SCO-training-09032012.txt", "SEM-2012-SharedTask-CD-SCO-dev-09032012.txt","SEM-2012-SharedTask-CD-SCO-test-merge-GOLD.txt" });
			//put("cdsco_en", new String[] { "trial.txt", "trial.txt","trial.txt" });
					/* "trial.txt", */ 
			put("cdsco2_en", new String[] { "SEM-2012-SharedTask-CD-SCO-training-09032012.txt", "SEM-2012-SharedTask-CD-SCO-dev-09032012.txt", "SEM-2012-SharedTask-CD-SCO-test-merge-GOLD.txt" });

			put("simple_wiki_en", new String[] { "", "[*]_exp.conll", "[*]_exp.conll" });
			put("simple_wiki2_en", new String[] { "", "[*]_exp.conll", "[*]_exp.conll" });

			put("simple_wiki_suffixal_en", new String[] { "", "suffixal_exp.conll", "suffixal_exp.conll" });
			put("cdsco_suffixal_en", new String[] { "SEM-2012-SharedTask-CD-SCO-training-09032012.txt", "suffixal_exp.conll", "suffixal_exp.conll" });

			put("simple_wiki_suffixal2_en", new String[] { "", "suffixal_exp.conll", "suffixal_exp.conll" });
			put("cdsco_suffixal2_en", new String[] { "SEM-2012-SharedTask-CD-SCO-training-09032012.txt", "suffixal_exp.conll", "suffixal_exp.conll" });

			put("simple_wiki_unseen2_en", new String[] { "", "unseen_exp.conll", "unseen_exp.conll" });
			put("cdsco_unseen2_en", new String[] { "SEM-2012-SharedTask-CD-SCO-training-09032012.txt", "unseen_exp.conll", "unseen_exp.conll" });

			
			put("cdsco-end2end_en", new String[] { "SEM-2012-SharedTask-CD-SCO-training-09032012.txt", "SEM-2012-SharedTask-CD-SCO-dev-09032012.txt",
			/* "trial.txt", */ "SEM-2012-SharedTask-CD-SCO-test-merge-GOLD.txt" });
			/*
			 * put("simple_wiki_simple_en", new String[]{"", "simple_exp.conll",
			 * "simple_exp.conll"}); put("simple_wiki_prefixal_en", new
			 * String[]{"", "prefixal_exp.conll", "prefixal_exp.conll"});
			 * put("simple_wiki_suffixal_en", new String[]{"",
			 * "suffixal_exp.conll", "suffixal_exp.conll"});
			 * put("simple_wiki_multiword_en", new String[]{"", "mw_exp.conll",
			 * "mw_exp.conll"}); put("simple_wiki_lexical_en", new String[]{"",
			 * "lexical_exp.conll", "lexical_exp.conll"});
			 * put("simple_wiki_genre_en", new String[]{"", "genre_exp.conll",
			 * "genre_exp.conll"}); put("simple_wiki_unseen_en", new
			 * String[]{"", "unseen_exp.conll", "unseen_exp.conll"});
			 * 
			 * put("simple_wiki_simple2_en", new String[]{"",
			 * "simple_exp.conll", "simple_exp.conll"});
			 * put("simple_wiki_prefixal2_en", new String[]{"",
			 * "prefixal_exp.conll", "prefixal_exp.conll"});
			 * put("simple_wiki_suffixal2_en", new String[]{"",
			 * "suffixal_exp.conll", "suffixal_exp.conll"});
			 * put("simple_wiki_multiword2_en", new String[]{"", "mw_exp.conll",
			 * "mw_exp.conll"}); put("simple_wiki_lexical2_en", new String[]{"",
			 * "lexical_exp.conll","lexical_exp.conll"});
			 * put("simple_wiki_genre2_en", new String[]{"", "genre_exp.conll",
			 * "genre_exp.conll"}); put("simple_wiki_unseen2_en", new
			 * String[]{"", "unseen_exp.conll", "unseen_exp.conll"});
			 */

			put("bioscope_abstracts_en", new String[] { "abstracts.train.[*].txt", "abstracts.test.[*].txt", "abstracts.test.[*].txt" });
			put("bioscope_full_en", new String[] { "abstracts.txt", "full_papers.txt", "full_papers.txt" });
			//put("bioscope_full_en", new String[] { "abstracts.txt", "full_papers0.txt", "full_papers0.txt" });
			put("bioscope_clinic_en", new String[] { "abstracts.txt", "clinical_records.txt", "clinical_records.txt" });
			
			put("bioscope_abstracts-end2end_en",  new String[] { "abstracts.train.[*].txt", "abstracts.test.[*].txt", "abstracts.test.[*].txt" });
			put("bioscope_full-end2end_en", new String[] { "abstracts.txt", "full_papers.txt", "full_papers.txt" });
			//put("bioscope_full-end2end_en", new String[] { "abstracts.txt", "full_papers0.txt", "full_papers0.txt" });
			put("bioscope_clinic-end2end_en", new String[] { "abstracts.txt", "clinical_records.txt", "clinical_records.txt" });

			put("cnesp_financial_cn", new String[] { "Financial_article.train.txt", "Financial_article.dev.txt", "Financial_article.test.[*].txt" });
			put("cnesp_product_cn", new String[] { "Product_review.train.txt", "Product_review.dev.txt", "Product_review.test.[*].txt" });
			put("cnesp_scientific_cn", new String[] { "Scientific_literature.train.txt", "Scientific_literature.dev.txt", "Scientific_literature.test.[*].txt" });

			put("cnesp_product-end2end_cn", new String[] { "Product_review.train.txt", "Product_review.dev.txt", "Product_review.test.[*].txt" });
			
			put("cnesp_product_all_cn", new String[] { "Product_review.txt", "", ""});
		}
	};

	public static String[] NineFold = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
	public static String[] TenFold = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

	public static final HashMap<String, String[]> dataset2Fold = new HashMap<String, String[]>() {
		{
			put("bioscope_abstracts_en", NineFold);
			
			put("bioscope_abstracts-end2end_en", NineFold);
			
			put("simple_wiki_en", new String[] { "simple", "lexical", "prefixal", "suffixal", "mw", "unseen" });
			put("simple_wiki2_en", new String[] { "simple", "lexical", "prefixal", "suffixal", "mw", "unseen" });

			put("cnesp_financial_cn", TenFold);
			put("cnesp_product_cn", TenFold);
			put("cnesp_scientific_cn", TenFold);
			
			put("cnesp_product-end2end_cn", TenFold);

		}
	};

	public static String pathJoin(String path, String filename) {
		return path + "//" + filename;
	}

	public static String[] getDataFiles(String dataSet, String lang) {
		return dataset2Files.get(dataSet + "_" + lang);
	}

	public static String[] getDataFold(String dataSet, String lang) {
		return dataset2Fold.get(dataSet + "_" + lang);
	}

	public static String getDataPath(String dataSet, String lang) {
		return pathJoin(DATA, dataset2Path.get(dataSet + "_" + lang));
	}

}
