#DIR_TRANS=/local2/vsoto/NIST/1A/BUILD/conversational/training/transcription
#FILE_COUNTS=/local2/vsoto/massive_scraping/1A/initial_counts.txt
#java -jar target/Babler-1.0.jar --init_bbn --train_set $DIR_TRANS --ranked_list $FILE_COUNTS

#DIR_TRANS=/local2/vsoto/NIST/1B/BUILD/conversational/training/transcription
#FILE_COUNTS=/local2/vsoto/massive_scraping/1B/initial_counts.txt
#java -jar target/Babler-1.0.jar --init_bbn --train_set $DIR_TRANS --ranked_list $FILE_COUNTS

DIR_TRANS=/local2/vsoto/NIST/1S/BUILD/conversational/training/transcription
FILE_COUNTS=/local2/vsoto/massive_scraping/1S/initial_counts.txt
java -jar target/Babler-1.0.jar --init_bbn --train_set $DIR_TRANS --ranked_list $FILE_COUNTS
