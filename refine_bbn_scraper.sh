#DIR_TRANS=/local2/vsoto/NIST/1A/BUILD/conversational/training/transcription
#INIT_FILE_COUNTS=/local2/vsoto/massive_scraping/1A/initial_counts.txt
#REFINED_FILE_COUNTS=/local2/vsoto/massive_scraping/1A/refined_counts.txt
#java -jar target/Babler-1.0.jar --config swa_bbn.properties --refine_bbn --train_set $DIR_TRANS --ranked_list $INIT_FILE_COUNTS --rescored_ranked_list $REFINED_FILE_COUNTS > refined_swa.log 

#DIR_TRANS=/local2/vsoto/NIST/1B/BUILD/conversational/training/transcription
#INIT_FILE_COUNTS=/local2/vsoto/massive_scraping/1B/initial_counts.txt
#REFINED_FILE_COUNTS=/local2/vsoto/massive_scraping/1B/refined_counts.txt
#java -jar target/Babler-1.0.jar --config tgl_bbn.properties --refine_bbn --train_set $DIR_TRANS --ranked_list $INIT_FILE_COUNTS --rescored_ranked_list $REFINED_FILE_COUNTS > refined_tgl.log

DIR_TRANS=/local2/vsoto/NIST/1S/BUILD/conversational/training/transcription
INIT_FILE_COUNTS=/local2/vsoto/massive_scraping/1S/initial_counts.txt
REFINED_FILE_COUNTS=/local2/vsoto/massive_scraping/1S/refined_counts.txt
java -jar target/Babler-1.0.jar --config som_bbn.properties --refine_bbn --train_set $DIR_TRANS --ranked_list $INIT_FILE_COUNTS --rescored_ranked_list $REFINED_FILE_COUNTS > refined_som.log


