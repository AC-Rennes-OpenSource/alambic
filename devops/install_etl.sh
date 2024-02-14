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
export ALAMBIC_TARGET_ENVIRONMENT="@globals.alambic_target_environment@"

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
ETL_VERSION=""
DO_FORCE_INSTALL=false

ALAMBIC_CONFIG_MULTITHREADING_POOL_SIZE=@option.alambic.thread.poolsize@
ALAMBIC_CONFIG_PERSISTENCE_UNIT="@option.alambic.persistence.unit.name@"
ALAMBIC_CONFIG_JDBC_DRIVER="@option.alambic.jdbc.driver@"
ALAMBIC_CONFIG_JDBC_URL="@option.alambic.jdbc.url@"
ALAMBIC_CONFIG_JDBC_USER="@option.alambic.jdbc.user@"
ALAMBIC_CONFIG_JDBC_PASSWORD="@option.alambic.jdbc.password@"
ALAMBIC_CONFIG_SECURITY_KEYSTORE_ALIAS="@option.alambic.security.keystore.alias@"
ALAMBIC_CONFIG_SECURITY_KEYSTORE_PASSWORD="@option.alambic.security.keystore.password@"
ALAMBIC_CONFIG_SECURITY_KEYSTORE_ALIAS_PASSWORD="@option.alambic.security.keystore.alias.password@"
ALAMBIC_GITLAB_CREDENTIALS_URL="@option.alambic.credentials.url@"
ALAMBIC_NEXUS_ARTIFACT_URL="@option.alambic.artefact.url@"
ALAMBIC_GITLAB_CREDENTIALS_VERSION=""

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
    echo "Usage: \"$0 -v <The version of the Alambic's product> -s <The version of the Alambic's credential files> [-x <true: do force install, as default: false>]\""
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
    
    if [ -z "${ETL_VERSION}" ]
    then
        logger "ERROR" "Invalid argument: '-v' must be set but is empty"
        IS_ERROR_STATUS=true
    fi

    if [ -z "${ALAMBIC_GITLAB_CREDENTIALS_VERSION}" ]
    then
        logger "ERROR" "Invalid argument: '-s' must be set but is empty"
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
    ACTIVE_LINK=$(ls -l ${ALAMBIC_HOME}/opt/etl/active 2>/dev/null | sed -r 's#.+active[^/]+'${ALAMBIC_HOME}'/opt/etl/tags/([^/]+).*#\1#')
    if [[ "${ACTIVE_LINK}" == "${ETL_VERSION}" ]]
    then
        logger "INFO" "La version '${ETL_VERSION}' est déjà installée"
        finally 0
    fi
    
    mkdir -p "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}"

    if [[ ! -f "${ALAMBIC_HOME}/alambic-product-${ETL_VERSION}.zip" ]]
    then
        NEXUS_DOWNLOAD_URL=$(echo "${ALAMBIC_NEXUS_ARTIFACT_URL}" | sed -r "s/#VERSION#/${ETL_VERSION}/g")
        if [[ "${ETL_VERSION}" =~ .*SNAPSHOT.* ]]
        then
            NEXUS_DOWNLOAD_URL=$(echo "${NEXUS_DOWNLOAD_URL}" | sed -r "s#maven-releases-aca-rennes#maven-snapshots-aca-rennes#")
        fi
        logger "INFO" "Téléchargement du livrable '${NEXUS_DOWNLOAD_URL}'"
        wget -q -P "${ALAMBIC_HOME}" "${NEXUS_DOWNLOAD_URL}"
    fi
    
    logger "INFO" "Extraction du livrable '${ALAMBIC_HOME}/alambic-product-${ETL_VERSION}.zip'"
    if [[ -f "${ALAMBIC_HOME}/alambic-product-${ETL_VERSION}.zip" ]]
    then
        unzip -d "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}" "${ALAMBIC_HOME}/alambic-product-${ETL_VERSION}.zip"
        mv ${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/scripting/* ${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}
        rm -rf ${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/scripting
    else
        logger "ERROR" "Erreur pendant le téléchargement du livrable '${NEXUS_DOWNLOAD_URL}'"
        finally 1
    fi
    
    logger "INFO" "Positionnement des droits"
    for item in `ls ${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/*.sh`
    do
        chmod 775 "${item}"
    done

    logger "INFO" "Supprimer l'archive de livrable"
    rm -f "${ALAMBIC_HOME}/alambic-product-${ETL_VERSION}.zip"

    logger "INFO" "Positionnement du lien symbolique pour désigner la version active '${ETL_VERSION}'"
    rm -f "${ALAMBIC_HOME}/opt/etl/active"
    ln -s "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}" "${ALAMBIC_HOME}/opt/etl/active"

    logger "INFO" "Installation du fichier de versionning"
    echo "alambic.version=${ETL_VERSION}" > "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/alambic.properties"
}

configure_version() {
    sed -r -i 's#repository.variables=(.+)#repository.variables='${ALAMBIC_HOME}'/opt/etl/tags/'${ETL_VERSION}'/conf/variables.xml#' "${ALAMBIC_HOME}/opt/etl/active/conf/config.properties"
    sed -r -i 's#repository.security.properties=(.+)#repository.security.properties='${ALAMBIC_HOME}'/opt/etl/tags/'${ETL_VERSION}'/conf/security.properties#' "${ALAMBIC_HOME}/opt/etl/active/conf/config.properties"
    sed -r -i 's#multithreading.pool.size=(.+)#multithreading.pool.size='${ALAMBIC_CONFIG_MULTITHREADING_POOL_SIZE}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/config.properties"
    sed -r -i 's#etl.persistence.unit=(.+)#etl.persistence.unit='${ALAMBIC_CONFIG_PERSISTENCE_UNIT}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/config.properties"
    sed -r -i 's#etl.jdbc.driver=(.+)#etl.jdbc.driver='${ALAMBIC_CONFIG_JDBC_DRIVER}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/config.properties"
    sed -r -i 's#etl.jdbc.url=(.+)#etl.jdbc.url='${ALAMBIC_CONFIG_JDBC_URL}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/config.properties"
    sed -r -i 's#etl.jdbc.user=(.+)#etl.jdbc.user='${ALAMBIC_CONFIG_JDBC_USER}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/config.properties"
    sed -r -i 's#etl.jdbc.password=(.+)#etl.jdbc.password='${ALAMBIC_CONFIG_JDBC_PASSWORD}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/config.properties"

    sed -r -i 's#repository.keystore=(.*)#repository.keystore='${ALAMBIC_HOME}'/opt/etl/tags/'${ETL_VERSION}'/conf/alambic.keystore#' "${ALAMBIC_HOME}/opt/etl/active/conf/security.properties"
    sed -r -i 's#(%alias%)#'${ALAMBIC_CONFIG_SECURITY_KEYSTORE_ALIAS}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/security.properties"
    sed -r -i 's#repository.keystore.password=(.*)#repository.keystore.password='${ALAMBIC_CONFIG_SECURITY_KEYSTORE_PASSWORD}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/security.properties"
    sed -r -i 's#repository.keystore.password.'${ALAMBIC_CONFIG_SECURITY_KEYSTORE_ALIAS}'=(.*)#repository.keystore.password.'${ALAMBIC_CONFIG_SECURITY_KEYSTORE_ALIAS}'='${ALAMBIC_CONFIG_SECURITY_KEYSTORE_ALIAS_PASSWORD}'#' "${ALAMBIC_HOME}/opt/etl/active/conf/security.properties"
}

install_credentials() {
    if [ ! -s "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/conf/variables.xml" ]
    then
        GITLAB_DOWNLOAD_URL=$(echo "${ALAMBIC_GITLAB_CREDENTIALS_URL}" | sed "s/#CREDENTIAL_FILE#/variables.xml/")
        GITLAB_DOWNLOAD_URL=$(echo "${GITLAB_DOWNLOAD_URL}" | sed "s/#CREDENTIAL_VERSION#/${ALAMBIC_GITLAB_CREDENTIALS_VERSION}/g")
        wget --quiet -O "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/conf/variables.xml" --header "Private-Token: @option.alambic.gitlab.pat@" ${GITLAB_DOWNLOAD_URL}
        if [ ! 0 -eq $? ]
        then
            logger "ERROR" "Echec lors de la récupération de la version ${ALAMBIC_GITLAB_CREDENTIALS_VERSION} du fichier 'variables.xml'"
            finally 1
        fi
    else
        logger "INFO" "Le fichier '${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/conf/variables.xml' est déjà présent"
    fi
    
    if [ ! -s "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/conf/alambic.keystore" ]
    then
        GITLAB_DOWNLOAD_URL=$(echo "${ALAMBIC_GITLAB_CREDENTIALS_URL}" | sed "s/#CREDENTIAL_FILE#/alambic.keystore/")
        GITLAB_DOWNLOAD_URL=$(echo "${GITLAB_DOWNLOAD_URL}" | sed "s/#CREDENTIAL_VERSION#/${ALAMBIC_GITLAB_CREDENTIALS_VERSION}/g")
        wget --quiet -O "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/conf/alambic.keystore" --header 'Private-Token: @option.alambic.gitlab.pat@' ${GITLAB_DOWNLOAD_URL}
        if [ ! 0 -eq $? ]
        then
            logger "ERROR" "Echec lors de la récupération de la version ${ALAMBIC_GITLAB_CREDENTIALS_VERSION} du fichier 'alambic.keystore'"
            finally 1
        fi
    else
        logger "INFO" "Le fichier '${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}/conf/alambic.keystore' est déjà présent"
    fi
}

#----------------------------------------------------------------------------
# Get the command options
#----------------------------------------------------------------------------
if [ $# -ge 1 ]
then
    # parse the command options
    while getopts "x:v:s:" opt
    do
    case $opt in
        v)
            ETL_VERSION=$OPTARG
            ;;
        x)
            DO_FORCE_INSTALL=$OPTARG
            ;;
        s)
            ALAMBIC_GITLAB_CREDENTIALS_VERSION=$OPTARG
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
mkdir -p "${ALAMBIC_HOME}/opt/etl/tags"
mkdir -p "${ALAMBIC_HOME}/opt/etl/logs"
mkdir -p "${ALAMBIC_HOME}/opt/etl/addons"

if [[ -d "${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}" ]] && ([[ "${ETL_VERSION}" =~ .*SNAPSHOT.* ]] || [[ true == ${DO_FORCE_INSTALL} ]])
then
    logger "INFO" "Suppression de la version précédente '${ETL_VERSION}'"
    rm -rf ${ALAMBIC_HOME}/opt/etl/tags/${ETL_VERSION}
    rm -f ${ALAMBIC_HOME}/opt/etl/active
fi

logger "INFO" "Installation de la version d'ETL '${ETL_VERSION}'"
install_version

logger "INFO" "Configuration de la version d'ETL"
configure_version

logger "INFO" "Installation des credentials"
install_credentials
 
for item in `find ${ALAMBIC_HOME}/opt/etl/tags/ -maxdepth 1 -type d | grep -v -E "(.*tags/$|${ETL_VERSION})"`
do
    logger "INFO" "Suppression de la version précédente d'ETL '${item}"
    rm -rf "${item}";
done

finally 0
