#!/bin/bash
#----------------------------------------------------------------------------
# Copyright (C) 2019-2020 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#----------------------------------------------------------------------------

#----------------------------------------------------------------------------
# Environment variables
# These variables deal with the environment execution context, whatever the treatment to run is. They are all required.
#
# - ALAMBIC_HOME : root path of Alambic deployment (engine & addons). Usualy set to '/opt/etl'.
# - ALAMBIC_LOG_DIR : the log directory. Usualy set to '/var/log/alambic/'.
# - ALAMBIC_LOG_AGE : how many days the log files will be kept within the Alambic log directory. Usualy set to 7 days.
# - ALAMBIC_TARGET_ENVIRONMENT : specifies the current execution environment context (development, qualification, staging, production...).
# - ALAMBIC_DEBUG_JVM_VARS : specifies the JVM execution variables to set to enable the debug mode. As default : -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y
#----------------------------------------------------------------------------
export ALAMBIC_HOME="@node.alambic_home@"
export ALAMBIC_LOG_DIR="@node.alambic_log_dir@"
export ALAMBIC_LOG_AGE="@node.alambic_log_age@"
export ALAMBIC_TARGET_ENVIRONMENT="@node.alambic_target_environment@"

#----------------------------------------------------------------------------
# Execution variables
# These variables deal with the specific treatment to run
#
# - DEBUG_MODE : optional - whether to enable the debug mode or not. 'false' as default.
# - CLEAN_OUTPUT_DIRECTORY : optional - whether to clean the addon's output directory or not. 'false' as default.
# - ALAMBIC_ADDON_NAME : required - the addon defining the treatment to run.
# - ALAMBIC_ADDON_JOB_FILE_NAME : optional - the file that defines the job to execute, 'jobs.xml' as default.
# - ALAMBIC_ADDON_JOB_NAME : optional - the name of the job to execute, 'all' as default.
# - JOB_PARAMETERS : optional - the job's parameters, none as default.
#----------------------------------------------------------------------------
VERBOSE=0
DEBUG_MODE=false
CLEAN_OUTPUT_DIRECTORY=false
ALAMBIC_HOME_PRODUCT="${ALAMBIC_HOME}/opt/etl/active"
ALAMBIC_HOME_ADDONS="${ALAMBIC_HOME}/opt/etl/addons"
ALAMBIC_ADDON_NAME=""
ALAMBIC_ADDON_JOB_FILE_NAME="jobs.xml"
ALAMBIC_ADDON_JOB_NAME="all"
JOB_PARAMETERS=""
ALAMBIC_DEBUG_JVM_VARS=""
DEFAULT_DEBUG_JVM_PARAMS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"

#----------------------------------------------------------------------------
# functions
#----------------------------------------------------------------------------
logger() {
    if [ "ERROR" = $1 ] || [ $VERBOSE -eq 2 ] || [ $1 = "INFO" -a $VERBOSE -eq 1 ]
    then
        echo "[$1]" $2
    fi
}

usage() {
    echo "Usage: \"$0 -n <The Alambic addon's name> [-f <the file that defines the job, as default 'jobs.xml'> -j <the name of the job to execute, as default 'all'> -p <the job's parameters> -c <true : clean the addon's output directory, as default : false> -v <set verbose level [0=error only, 1=info+error, 2=all], as default : 0> -d <true : enable the debug mode, as default : false>]\""
}

finally() {
    logger "INFO" "End of treatment"
    unset ALAMBIC_HOME
    unset ALAMBIC_LOG_DIR
    unset ALAMBIC_LOG_AGE
    unset ALAMBIC_TARGET_ENVIRONMENT
    exit $1
}

before_start() {
    IS_ERROR_STATUS=false

    if [[ ! "${VERBOSE}" =~ ^(0|1|2)$ ]]
    then
        logger "ERROR" "Invalid argument: '-v' must fit one of the values [0,1,2]"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${ALAMBIC_ADDON_NAME}" ]
    then
        logger "ERROR" "Invalid argument: '-n' must be set but is empty"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${ALAMBIC_HOME}" ]
    then
        logger "ERROR" "The environment variable 'ALAMBIC_HOME' must be set but is empty"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${ALAMBIC_LOG_DIR}" ]
    then
        logger "ERROR" "The environment variable 'ALAMBIC_LOG_DIR' must be set but is empty"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${ALAMBIC_LOG_AGE}" ]
    then
        logger "ERROR" "The environment variable 'ALAMBIC_LOG_AGE' must be set but is empty"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${ALAMBIC_TARGET_ENVIRONMENT}" ]
    then
        logger "ERROR" "The environment variable 'ALAMBIC_TARGET_ENVIRONMENT' must be set but is empty"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${ALAMBIC_DEBUG_JVM_VARS}" ] && [ true == ${DEBUG_MODE} ]
    then
        logger "INFO" "Set the 'ALAMBIC_DEBUG_JVM_VARS' to the default value ${DEFAULT_DEBUG_JVM_PARAMS}"
        ALAMBIC_DEBUG_JVM_VARS="${DEFAULT_DEBUG_JVM_PARAMS}"
    fi

    if [ true == ${IS_ERROR_STATUS} ]
    then
        usage
        finally 1
    fi
}

#----------------------------------------------------------------------------
# Get the command options
#----------------------------------------------------------------------------
if [ $# -ge 1 ]
then
    # parse the command options
    while getopts "d:v:n:c:f:j:p:D:" opt
    do
    case $opt in
        d)
            # enable debug mode
            DEBUG_MODE=$OPTARG
            ;;
        v)
            # enable verbose logs
            VERBOSE=$OPTARG
            ;;
        n)
            # Alambic addon name
            ALAMBIC_ADDON_NAME=$OPTARG
            ;;
        c)
            # enable debug mode
            CLEAN_OUTPUT_DIRECTORY=$OPTARG
            ;;
        f)
            # Alambic addon job file name to execute
            ALAMBIC_ADDON_JOB_FILE_NAME=$OPTARG
            ;;
        j)
            # Alambic addon job name to execute
            ALAMBIC_ADDON_JOB_NAME=$OPTARG
            ;;
        p)
            # Alambic addon job's parameters
            JOB_PARAMETERS=$OPTARG
            ;;
        D)
            # Set the debug JVM parameters
            ALAMBIC_DEBUG_JVM_VARS=$OPTARG
            ;;
        \?)
            logger "ERROR" "Invalid argument: -$OPTARG is not supported" >&2
            usage
            finally 1
            ;;
    esac
    done
fi

#----------------------------------------------------------------------------
# Check the command options and variables
#----------------------------------------------------------------------------
before_start

echo "ALAMBIC_HOME=${ALAMBIC_HOME}"
echo "ALAMBIC_HOME_PRODUCT=${ALAMBIC_HOME_PRODUCT}"
echo "ALAMBIC_HOME_ADDONS=${ALAMBIC_HOME_ADDONS}"
echo "ALAMBIC_LOG_DIR=${ALAMBIC_LOG_DIR}"
echo "ALAMBIC_LOG_AGE=${ALAMBIC_LOG_AGE}"
echo "ALAMBIC_TARGET_ENVIRONMENT=${ALAMBIC_TARGET_ENVIRONMENT}"
echo "ALAMBIC_ADDON_NAME=${ALAMBIC_ADDON_NAME}"
echo "ALAMBIC_ADDON_JOB_FILE_NAME=${ALAMBIC_ADDON_JOB_FILE_NAME}"
echo "ALAMBIC_ADDON_JOB_NAME=${ALAMBIC_ADDON_JOB_NAME}"
echo "CLEAN_OUTPUT_DIRECTORY=${CLEAN_OUTPUT_DIRECTORY}"
echo "DEBUG_MODE=${DEBUG_MODE}"
echo "JOB_PARAMETERS=${JOB_PARAMETERS}"

#----------------------------------------------------------------------------
# Prepare the runner instance and execute
#----------------------------------------------------------------------------
logger "INFO" "Start of treatment"
if [ ! -d "${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/output" ]
then
    logger "INFO" "Create the addon's output directory since it doesn't exist"
    mkdir ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/output
fi

if [ true == ${CLEAN_OUTPUT_DIRECTORY} ]
then
    logger "INFO" "Clean the addon output directory"
    rm -rf ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/output/*
else
    logger "INFO" "Keep the addon output directory"
fi

logger "INFO" "Prepare the runner instance"
cp -f ${ALAMBIC_HOME_PRODUCT}/runner.sh ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/jobs/runner.sh
chmod 554 ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/jobs/runner.sh

logger "INFO" "Update the execution path for run"
sed -r -i 's#(EXECUTION_PATH=")[^"]+(";)#\1../../../active/\2#' ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/jobs/runner.sh

logger "INFO" "Update the addon name"
sed -r -i 's#(ADDON_NAME=")[^"]*(")#\1'''${ALAMBIC_ADDON_NAME}'''\2#' ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/jobs/runner.sh

logger "INFO" "Remove the runner log file"
rm -rf ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/jobs/runner.log

cd ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/jobs
if [ true == ${DEBUG_MODE} ]
then
    logger "INFO" "Run the script ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/jobs/${ALAMBIC_ADDON_JOB_FILE_NAME} in debug mode"
    ./runner.sh -v -f "${ALAMBIC_ADDON_JOB_FILE_NAME}" -j "${ALAMBIC_ADDON_JOB_NAME}" -D "${ALAMBIC_DEBUG_JVM_VARS}" -p "${JOB_PARAMETERS}" > runner.log 2>&1
else
    logger "INFO" "Run the script ${ALAMBIC_HOME_ADDONS}/${ALAMBIC_ADDON_NAME}/jobs/${ALAMBIC_ADDON_JOB_FILE_NAME}"
    ./runner.sh -v -f "${ALAMBIC_ADDON_JOB_FILE_NAME}" -j "${ALAMBIC_ADDON_JOB_NAME}" -p "${JOB_PARAMETERS}" > runner.log 2>&1
fi

finally 0
