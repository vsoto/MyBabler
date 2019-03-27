#REFINED_FILE_COUNTS=/local2/vsoto/massive_scraping/1A/refined_counts.txt
#java -jar target/Babler-1.0.jar -m bbn --lang swa --config swa_bbn.properties --rescored_ranked_list $REFINED_FILE_COUNTS > bbn_swa_log.txt

#REFINED_FILE_COUNTS=/local2/vsoto/massive_scraping/1B/refined_counts.txt
#java -jar target/Babler-1.0.jar -m bbn --lang tgl --config tgl_bbn.properties --rescored_ranked_list $REFINED_FILE_COUNTS > bbn_tgl_log.txt

REFINED_FILE_COUNTS=/local2/vsoto/massive_scraping/1S/refined_counts.txt
java -jar target/Babler-1.0.jar -m bbn --lang som --config som_bbn.properties --rescored_ranked_list $REFINED_FILE_COUNTS > bbn_som_log.txt
