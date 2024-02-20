#!/bin/bash

#----------------------------------------------
# README
#
# System variables are :
#- ALAMBIC_LOG_DIR : désigne l'emplacement des logs et rapports
#- ALAMBIC_NOTIFICATION_EMAIL_LIST : la liste d'adresse à utiliser pour la notification email
#- ALAMBIC_LOG_AGE : définit le nombre de jours par défaut de conservation des logs. Si 0, pas de suppression.
#
#----------------------------------------------

#----------------------------------------------
# Variables
#----------------------------------------------
EXECUTION_PATH="./";
INPUT_FILE=""
JOBS_LIST="all"
EXECUTE_ALL="FALSE"
VERBOSE=0
SUPERVISION_MODE="FALSE"
THREAD_POOL_SIZE="0" # '0' means : not definied, use the configuration file instead.
NOTIFICATION_THRESHOLD_NONE=0
NOTIFICATION_THRESHOLD_ERROR=1
NOTIFICATION_THRESHOLD_WARNING=2
NOTIFICATION_THRESHOLD_INFO=3
NOTIFICATION_THRESHOLD=${NOTIFICATION_THRESHOLD_ERROR}
NOTIFICATION_MAILING_LIST="${ALAMBIC_NOTIFICATION_EMAIL_LIST}"
CMD_PARAMS=""
# Heap sizing & Jolokia permanent additional JVM parameters
JVM_ADDITIONAL_PARAMS="-Xms512m -Xmx2g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -Dcom.sun.jndi.ldap.connect.pool.timeout=60000 -Dlog4j.configurationFile=${EXECUTION_PATH}/conf/log4j2.xml -javaagent:${EXECUTION_PATH}/lib/org.jolokia-jolokia-jvm.jar=port=8778,host=0.0.0.0"
# Debug optional additional JVM parameters
DEFAULT_DEBUG_JVM_PARAMS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"
# JMX optional additional JVM parameters (so that a tool like JVisualVM is used for deep application profiling - not Jolokia WEB end-point)
DEFAULT_JMX_JVM_PARAMS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
# Name of the currently executed addon
ADDON_NAME=""
# Max age of the addon logs
ADDON_LOG_AGE=${ALAMBIC_LOG_AGE}
# START/ENDING SCRIPT TIME (epoch reference)
START_TIME=$(date +'%s')
# SCRIPT OVERALL STATUS
SCRIPT_STATUS="avec succès"
SCRIPT_STATUS_CLASS="success"

#----------------------------------------------
# functions
#----------------------------------------------
logger() {
	if [ "ERROR" = $1 ] || [ "WARNING" = $1 ] || [ $VERBOSE -eq 2 ] || [ $1 = "INFO" -a $VERBOSE -eq 1 ]
	then
		echo "[$1]" $2
	fi
}

usage() {
	echo "Usage: \"$0 -n <the addon name> -f <jobs xml file> [-j (jobs list. ':' character as separator) -a (to execute all jobs) -s (supervision, a notification is done according to the threshold specified by -t parameter) -t (defines the notification threshold - values: NONE, INFO, WARNING, ERROR - ERROR as default) -m (mailing list - as default is set by the environment variable 'ALAMBIC_NOTIFICATION_EMAIL_LIST') -p (optional arguments for ETL execution) -c (the thread pool size. As default, use the configuration file's definition) -v (verbose) -d (debug) -D (debug with specific jvm parameters)]\""
}

setNotificationThreshold() {
	case $1 in
		NONE)
			NOTIFICATION_THRESHOLD=${NOTIFICATION_THRESHOLD_NONE}
			;;
		INFO)
			NOTIFICATION_THRESHOLD=${NOTIFICATION_THRESHOLD_INFO}
			;;
		WARNING)
			NOTIFICATION_THRESHOLD=${NOTIFICATION_THRESHOLD_WARNING}
			;;
		ERROR)
			NOTIFICATION_THRESHOLD=${NOTIFICATION_THRESHOLD_ERROR}
			;;
		*)
			logger "ERROR" "Invalid threshold argument: - $1" >&2
			usage
			finally 1
			;;
	esac
}

#----------------------------------------------
# Report
#
# Archive logs, build report and update audit
#----------------------------------------------
report() {
	# Create the addon log directory if missing
	if [ ! -e ${ALAMBIC_LOG_DIR}${ADDON_NAME} ]
	then
		mkdir "${ALAMBIC_LOG_DIR}${ADDON_NAME}"
	fi

	# Get ending script time and compute the script's duration
	END_TIME=$(date +'%s')
	EPOCH_DIFF=$(($END_TIME - $START_TIME))
	SCRIPT_DURATION=$(date +'%Hh:%Mm:%Ss' -ud @${EPOCH_DIFF})

	# Compute the report/log files unique process identifier
	NORMALIZED_JOBS_LIST=$(echo ${JOBS_LIST} | sed -r 's#\W+#-#g')
	NORMALIZED_INPUT_FILE=$(echo ${INPUT_FILE} | sed -r 's#(.*/)?([^/\.]+)(\..+)?#\2#')
	FILE_PROCESS_IDENTIFIER=$(echo "${PROCESS_IDENTIFIER}-${NORMALIZED_INPUT_FILE}-${NORMALIZED_JOBS_LIST}")

	# BACKUP RUNNER LOG FILE
	BACKUP_RUNNER_LOG_FILE="${ALAMBIC_LOG_DIR}${ADDON_NAME}/alambic-${FILE_PROCESS_IDENTIFIER}.log"

	# Keep backup of the addon's runner logs
	cp runner.log ${BACKUP_RUNNER_LOG_FILE}
	
	# Build output report (HTML format)
	REPORT_FILE="${ALAMBIC_LOG_DIR}${ADDON_NAME}/alambic-${FILE_PROCESS_IDENTIFIER}-report.html"
	logger "INFO" "Build the report file '${REPORT_FILE}'"

	# Get the Alambic version
	ALAMBIC_VERSION=$(grep "alambic.version=" ${EXECUTION_PATH}/alambic.properties | sed -r 's#.+=(.+)#\1#')

	# Get the addon version
	ADDON_VERSION=$(grep "addon.version=" ../addon.properties | sed -r 's#.+=(.+)#\1#')

	# extract the change logs from output ETL tool's log
	CHANGELOGS_CREATE=$(grep -c -E ".+ INFO .+(Creation)" ${BACKUP_RUNNER_LOG_FILE})
	CHANGELOGS_UPDATE=$(grep -c -E ".+ INFO .+(Modification|Execute the Nuxeo chain)" ${BACKUP_RUNNER_LOG_FILE})
	CHANGELOGS_DELETE=$(grep -c -E ".+ INFO .+(Suppression|Effacement)" ${BACKUP_RUNNER_LOG_FILE})
	CHANGELOGS_NOTIFY=$(grep -c -E ".+ INFO .+(Notification)" ${BACKUP_RUNNER_LOG_FILE})
	CHANGELOGS_COUNT=$(( ${CHANGELOGS_CREATE} + ${CHANGELOGS_UPDATE} + ${CHANGELOGS_DELETE} + ${CHANGELOGS_NOTIFY} ))

	# extract the warning logs from output ETL tool's log
	WARNINGS_COUNT=$(grep -c -E "( WARN )" ${BACKUP_RUNNER_LOG_FILE})
	
	# extract the error logs from output ETL tool's log
	ERRORS_COUNT=$(grep -c -E "(ERROR)" ${BACKUP_RUNNER_LOG_FILE})
	
	if [ $WARNINGS_COUNT -gt 0 ]
	then
		SCRIPT_STATUS="avec des warnings"
		SCRIPT_STATUS_CLASS="warning"
	fi

	if [ $ERRORS_COUNT -gt 0 ]
	then
		SCRIPT_STATUS="avec des erreurs"
		SCRIPT_STATUS_CLASS="error"
	fi

	# SED the content of the report based-on the template report
	REPORT_DATE=$(date +"%d/%m/%Y %T")
	cp "${EXECUTION_PATH}/data/template/report-template.html" "${REPORT_FILE}"
	sed -i "s#@REPORT_FILE_TITLE#Rapport du script ${NORMALIZED_INPUT_FILE}.xml du addon ${ADDON_NAME}#g" "${REPORT_FILE}"
	sed -i "s#@SCRIPT_FILE#${NORMALIZED_INPUT_FILE}.xml#g" "${REPORT_FILE}"
	sed -i "s#@ADDON_NAME#${ADDON_NAME}#g" "${REPORT_FILE}"
	sed -i "s#@SCRIPT_STATUS_CLASS#${SCRIPT_STATUS_CLASS}#g" "${REPORT_FILE}"
	sed -i "s#@SCRIPT_STATUS#$(echo ${SCRIPT_STATUS} | sed 's#è#\&egrave;#')#g" "${REPORT_FILE}"
	sed -i "s#@JOBS_LIST#${JOBS_LIST}#g" "${REPORT_FILE}"
	sed -i "s#@DATE_EXECUTION#${REPORT_DATE}#g" "${REPORT_FILE}"
	sed -i "s#@CHANGELOGS_COUNT#${CHANGELOGS_COUNT}#g" "${REPORT_FILE}"
	sed -i "s#@CHANGELOGS_CREATE#${CHANGELOGS_CREATE}#g" "${REPORT_FILE}"
	sed -i "s#@CHANGELOGS_UPDATE#${CHANGELOGS_UPDATE}#g" "${REPORT_FILE}"
	sed -i "s#@CHANGELOGS_DELETE#${CHANGELOGS_DELETE}#g" "${REPORT_FILE}"
	sed -i "s#@CHANGELOGS_NOTIFY#${CHANGELOGS_NOTIFY}#g" "${REPORT_FILE}"
	sed -i "s#@WARNINGS_COUNT#${WARNINGS_COUNT}#g" "${REPORT_FILE}"
	sed -i "s#@ERRORS_COUNT#${ERRORS_COUNT}#g" "${REPORT_FILE}"
	sed -i "s#@SCRIPT_DURATION#${SCRIPT_DURATION}#g" "${REPORT_FILE}"
	sed -i "s#@LOGS_LOCATION#$(hostname):${BACKUP_RUNNER_LOG_FILE}#g" "${REPORT_FILE}"
	sed -i "s#@ALAMBIC_VERSION#${ALAMBIC_VERSION}#g" "${REPORT_FILE}"
	sed -i "s#@ADDON_VERSION#${ADDON_VERSION}#g" "${REPORT_FILE}"

	# Update the audit (CSV format)
	AUDIT_FILE="${ALAMBIC_LOG_DIR}${ADDON_NAME}/alambic-${ADDON_NAME}-audit.csv"
	logger "INFO" "Update the audit file '${AUDIT_FILE}'"
	
	# Build / append the addon's audit log
	if [ ! -e ${AUDIT_FILE} ]
	then
		echo "JOBS_DEFINITON_FILE;EXECUTION_DATE;EXECUTED_JOBS;DURATION;STATUS;CHANGELOGS_CREATE;CHANGELOGS_UPDATE;CHANGELOGS_DELETE;CHANGELOGS_NOTIFY;WARNINGS_COUNT;ERRORS_COUNT;ALAMBIC_VERSION;ADDON_VERSION" > ${AUDIT_FILE}
	fi
	echo "${NORMALIZED_INPUT_FILE}.xml;${REPORT_DATE};${JOBS_LIST};${EPOCH_DIFF};${SCRIPT_STATUS};${CHANGELOGS_CREATE};${CHANGELOGS_UPDATE};${CHANGELOGS_DELETE};${CHANGELOGS_NOTIFY};${WARNINGS_COUNT};${ERRORS_COUNT};${ALAMBIC_VERSION};${ADDON_VERSION}" >> ${AUDIT_FILE}

	if [ "TRUE" = "$SUPERVISION_MODE" ]
	then
		if [ -n "${NOTIFICATION_MAILING_LIST}" ]
		then
			if [ $ERRORS_COUNT -gt 0 -a $NOTIFICATION_THRESHOLD -ge 1 ] || [ $WARNINGS_COUNT -gt 0 -a $NOTIFICATION_THRESHOLD -ge 2 ] || [ ${CHANGELOGS_COUNT} -gt 0 -a $NOTIFICATION_THRESHOLD -ge 3 ]
			then
				cp ${EXECUTION_PATH}/runner-email-template.txt runner-email.txt
				if [ $ERRORS_COUNT -gt 0 ]
				then
					echo "Here is a sample of the error logs :" > runner-email.txt
					grep -m 10 -E "(ERROR|\s+at .+)" ${BACKUP_RUNNER_LOG_FILE} >> runner-email.txt
				fi

				if [ $WARNINGS_COUNT -gt 0 ]
				then
					if [ $ERRORS_COUNT -eq 0 ]
					then
						echo "Here is a sample of the warning logs :" > runner-email.txt
					else
						echo " " >> runner-email.txt
						echo "Here is a sample of the warning logs :" >> runner-email.txt
					fi
					grep -m 10 -E "( WARN )" ${BACKUP_RUNNER_LOG_FILE} >> runner-email.txt
				fi

				mail -r "noreply@ac-rennes.fr" -s "[NOTIFICATION ALAMBIC - ${ALAMBIC_TARGET_ENVIRONMENT}] Addon ${ADDON_NAME} exécuté ${SCRIPT_STATUS}" "${NOTIFICATION_MAILING_LIST}" < runner-email.txt
			fi
		else
			# The environment variable that sets the mailing list is missing
			logger "ERROR" "The notification mailing list is missing"
		fi
	fi
}

#----------------------------------------------
# Flush
#
# Flush the produced logs and reports based-on the system variable "ALAMBIC_LOG_AGE"
#----------------------------------------------
flush() {
	if [ $ADDON_LOG_AGE -gt 0 ]
	then
		# list all files to flush
	    find "${ALAMBIC_LOG_DIR}${ADDON_NAME}/" -maxdepth 1 -type f -mtime +${ADDON_LOG_AGE} -iname "*.log" | xargs rm -f
	    find "${ALAMBIC_LOG_DIR}${ADDON_NAME}/" -maxdepth 1 -type f -mtime +${ADDON_LOG_AGE} -iname "*.html" | xargs rm -f
		logger "INFO" "Suppression des logs depuis le dossier '${ALAMBIC_LOG_DIR}${ADDON_NAME}' ayant une ancienneté >= ${ADDON_LOG_AGE} jours"
	else
		logger "WARNING" "La suppression des logs est désactivée (age des logs configuré est '${ADDON_LOG_AGE}' jours, actif seulement si > 0)"
	fi
}

finally() {
	logger "INFO" "Fin de traitement du script"
	exit $1
}

#---------------------------------------------
# Controls
#---------------------------------------------
{
if [ $# -ge 2 ]
then
	# parse the command options
	while getopts ":f:j:l:t:m:p:avdxD:c:sn:" opt
	do
		case $opt in
			f)
				# get the input file
				INPUT_FILE=$OPTARG
				if [ ! -s $INPUT_FILE ]
				then
					logger "ERROR" "Le fichier '$INPUT_FILE' est vide ou n'existe pas"
					usage
					finally 1
				fi
				;;
			j)
				# get the jobs to execute
				JOBS_LIST=$OPTARG
				;;
			l)
				# get the addon log age
				ADDON_LOG_AGE=$OPTARG
				;;
			a)
				# will execute all jobs
				EXECUTE_ALL="TRUE"
				;;
			v)
				# enable verbose simple logs
				VERBOSE=1
				;;
			d)
				# enable debug logs and enable debug mode
				VERBOSE=2
				JVM_ADDITIONAL_PARAMS="${JVM_ADDITIONAL_PARAMS} ${DEFAULT_DEBUG_JVM_PARAMS}"
				;;
			x)
				# enable JMX channel
				JVM_ADDITIONAL_PARAMS="${JVM_ADDITIONAL_PARAMS} ${DEFAULT_JMX_JVM_PARAMS}"
				;;
			D)
				# enable debug logs and enable debug mode (debug arguments are passed-in parameters)
				VERBOSE=2
				JVM_ADDITIONAL_PARAMS="${JVM_ADDITIONAL_PARAMS} ${OPTARG}"
				;;
			s)
				# enable supervision mode
				SUPERVISION_MODE="TRUE"
				;;
			t)
				# set the notification threshold
				setNotificationThreshold=$OPTARG
				;;
			m)
				# set the mailing list
				NOTIFICATION_MAILING_LIST=$OPTARG
				;;
			p)
				CMD_PARAMS=$OPTARG
				;;
			c)
				# set the thread pool size
				THREAD_POOL_SIZE=$OPTARG
				;;
			n)
				# get the addon name defining the job to execute
				ADDON_NAME=$OPTARG
				if [ -z $ADDON_NAME ]
				then
					logger "ERROR" "Invalid argument: '-n' must be set but is empty "
					usage
					finally 1
				fi

				# PROCESS IDENTIFIER
				PROCESS_IDENTIFIER="${ADDON_NAME}-$(date +'%Y%m%dT%H%M%S-%N')"
				;;
			\?)
				logger "ERROR" "Invalid argument: -$OPTARG is not supported" >&2
				usage
				finally 1
				;;
		esac
	done

	#---------------------------------------------
	# Execution
	#---------------------------------------------
	if [ -s "$INPUT_FILE" ]
	then
		if [ $VERBOSE -eq 2 ]
		then
			CMD_PARAMS="${CMD_PARAMS},debug_mode"
		fi

		if [ "TRUE" = "${EXECUTE_ALL}" ]
		then
			logger "INFO" "Lancement de tous les jobs (fichier d'entrée: '${INPUT_FILE}')"

			if [ "" != "$CMD_PARAMS" ]
			then
				PARAMETERS="$(echo ${CMD_PARAMS} | sed -r 's/,/ /g')"
				java ${JVM_ADDITIONAL_PARAMS} -jar ${EXECUTION_PATH}/bin/alambic.jar --execution-path=${EXECUTION_PATH} --thread-count=${THREAD_POOL_SIZE} -j="$INPUT_FILE" -ea -p="$PARAMETERS"
			else
				java ${JVM_ADDITIONAL_PARAMS} -jar ${EXECUTION_PATH}/bin/alambic.jar --execution-path=${EXECUTION_PATH} --thread-count=${THREAD_POOL_SIZE} -j="$INPUT_FILE" -ea
			fi
		else
			logger "INFO" "Lancement des jobs '$JOBS_LIST' (fichier d'entrée: '${INPUT_FILE}')"

			if [ "" != "$CMD_PARAMS" ]
			then
				PARAMETERS="$(echo ${CMD_PARAMS} | sed -r 's/,/ /g')"
				java ${JVM_ADDITIONAL_PARAMS} -jar ${EXECUTION_PATH}/bin/alambic.jar --execution-path=${EXECUTION_PATH} --thread-count=${THREAD_POOL_SIZE} -j="$INPUT_FILE" -el="$JOBS_LIST" -p="$PARAMETERS"
			else
				java ${JVM_ADDITIONAL_PARAMS} -jar ${EXECUTION_PATH}/bin/alambic.jar --execution-path=${EXECUTION_PATH} --thread-count=${THREAD_POOL_SIZE} -j="$INPUT_FILE" -el="$JOBS_LIST"
			fi
		fi

		logger "INFO" "Production du rapport d'exécution"
		report

		logger "INFO" "Gestion de l'ancienneté des logs et rapports"
		flush
	else
		logger "ERROR" "Le fichier '$INPUT_FILE' est vide ou n'existe pas"
		usage
		finally 1
	fi
else
	logger "ERROR" "Au moins une des options obligatoires est absente"
	usage
	finally 1
fi
} > runner.log
finally 0
