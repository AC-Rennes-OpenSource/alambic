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
#----------------------------------------------------------------------------
export ALAMBIC_HOME="@globals.alambic_home@"
export ALAMBIC_LOG_DIR="@globals.alambic_log_dir@"
export ALAMBIC_LOG_AGE="@globals.alambic_log_age@"
export ALAMBIC_TARGET_ENVIRONMENT="@globals.alambic_target_environement@"

#----------------------------------------------------------------------------
# Execution variables
# These variables deal with the specific treatment to run
#
# - DEBUG_MODE : optional - whether to enable the debug mode or not. 'false' as default.
# - CLEAN_OUTPUT_DIRECTORY : optional - whether to clean the addon's output directory or not. 'false' as default.
# - ALAMBIC_ADDON_NAME : required - the addon defining the treatment to run.
# - ALAMBIC_ADDON_SCRIPT_FILE_NAME : optional - the script file to execute, 'script.sh' as default.
# - SCRIPT_PARAMETERS : optional - the scipt's parameters, none as default.
#----------------------------------------------------------------------------
VERBOSE=2
ETL_VERBOSE=""
ETL_ADDON_VERSION=""
ETL_ADDON_NAME=""
DOWNLOAD_URL=""
DO_FORCE_INSTALL=false

#----------------------------------------------------------------------------
# TODO : to set into Rundeck config
#----------------------------------------------------------------------------

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
    echo "Usage: \"$0 -a <The Alambic's addon name (e.g. artifactid)> -v <The Alambic's addon version to install> -u <the URL to download the addon version archive> [-x <do force install>]\""
}

finally() {
    if [ $1 -eq 0 ]
    then
        logger "INFO" "Fin de l'installation en succès"
    else
        logger "ERROR" "Fin de l'installation en erreur"
    fi
    unset ALAMBIC_HOME
    unset ALAMBIC_LOG_DIR
    unset ALAMBIC_LOG_AGE
    unset ALAMBIC_TARGET_ENVIRONMENT
    exit $1
}

before_start() {
    IS_ERROR_STATUS=false
    
    if [ -z "${ETL_ADDON_NAME}" ]
    then
        logger "ERROR" "Invalid argument: '-a' must be set but is empty"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${ETL_ADDON_VERSION}" ]
    then
        logger "ERROR" "Invalid argument: '-v' must be set but is empty"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${DOWNLOAD_URL}" ]
    then
        logger "ERROR" "Invalid argument: '-u' must be set but is empty"
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

    if [ true == ${IS_ERROR_STATUS} ]
    then
        usage
        finally 1
    fi
}

install_version() {
    ACTIVE_LINK=$(ls -l ${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME} 2>/dev/null | sed -r 's#.+'${ETL_ADDON_NAME}'[^/]+'${ALAMBIC_HOME}'/opt/etl/addons/'${ETL_ADDON_NAME}'-([^/]+).*#\1#')
    if [[ "${ACTIVE_LINK}" == "${ETL_ADDON_VERSION}" ]]
    then
        logger "INFO" "La version '${ETL_ADDON_VERSION}' est déjà installée"
        finally 0
    fi

    mkdir -p "${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}"
    
    if [[ ! -f "${ALAMBIC_HOME}/opt/etl/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}.zip" ]]
    then
        logger "INFO" "Téléchargement du livrable '${DOWNLOAD_URL}'"
        wget -q -P "${ALAMBIC_HOME}/opt/etl" "${DOWNLOAD_URL}"
    fi
    
    logger "INFO" "Extraction du livrable '${DOWNLOAD_URL}'"
    if [[ -f "${ALAMBIC_HOME}/opt/etl/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}.zip" ]]
    then
        unzip -d "${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}" "${ALAMBIC_HOME}/opt/etl/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}.zip"
    else
        logger "ERROR" "Erreur pendant le téléchargement du livrable '${DOWNLOAD_URL}'"
        finally 1
    fi
    
    logger "INFO" "Positionnement des droits"
    for item in `ls ${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}/scripts/*.sh`
    do
        chmod 775 "${item}"
    done

    logger "INFO" "Positionnement du lien symbolique pour désigner la version active '${ETL_ADDON_VERSION}'"
    ln -s "${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}" "${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}"
    chmod 775 "${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}"

    logger "INFO" "Installation du fichier de versionning"
    echo "addon.version=${ETL_ADDON_VERSION}" >> "${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}/addon.properties"

    logger "INFO" "Supprimer l'archive de livrable"
    rm -f "${ALAMBIC_HOME}/opt/etl/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}.zip"
    rm -rf "${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}/devops"
}

#----------------------------------------------------------------------------
# Get the command options
#----------------------------------------------------------------------------
if [ $# -ge 1 ]
then
    # parse the command options
    while getopts "xv:u:a:" opt
    do
    case $opt in
        a)
            ETL_ADDON_NAME=$OPTARG
            ;;
        v)
            ETL_ADDON_VERSION=$OPTARG
            ;;
        u)
            DOWNLOAD_URL=$OPTARG
            ;;
        x)
            DO_FORCE_INSTALL=true
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
echo "ALAMBIC_LOG_DIR=${ALAMBIC_LOG_DIR}"
echo "ALAMBIC_LOG_AGE=${ALAMBIC_LOG_AGE}"
echo "ALAMBIC_TARGET_ENVIRONMENT=${ALAMBIC_TARGET_ENVIRONMENT}"

#----------------------------------------------------------------------------
# Run install
#----------------------------------------------------------------------------
logger "INFO" "Démarrage de l'installation"
logger "INFO" "Création de l'arborescence de déploiement"
mkdir -p "${ALAMBIC_HOME}/opt/etl/addons"

if [[ -d "${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}" ]] && ([[ "${ETL_ADDON_VERSION}" =~ .*SNAPSHOT.* ]] || [[ true == ${DO_FORCE_INSTALL} ]])
then
    logger "INFO" "Suppression de la version précédente '${ETL_ADDON_VERSION}'"
    rm -rf ${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}-${ETL_ADDON_VERSION}
    rm -f ${ALAMBIC_HOME}/opt/etl/addons/${ETL_ADDON_NAME}
fi

logger "INFO" "Installation de la version '${ETL_ADDON_VERSION}' du addon ETL '${ETL_ADDON_NAME}' si nécessaire"
install_version
 
for item in `find ${ALAMBIC_HOME}/opt/etl/addons/ -maxdepth 1 -type d -iname "*${ETL_ADDON_NAME}*" | grep -v -E "(.*addons/$|${ETL_ADDON_NAME}-${ETL_ADDON_VERSION})"`
do
    logger "INFO" "Suppression de la version précédente du addon d'ETL '${item}"
    rm -rf "${item}";
done

finally 0