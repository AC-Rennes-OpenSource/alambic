<#import "logging.macros.ftl" as LOG>

<#-- ****************************************************************************** -->
<#-- ********************************** MACROS ************************************ -->
<#-- ****************************************************************************** -->
<#macro WRITE_ENTRY_STORE_TO_CACHE entry>
	<#assign name = entry['name'][0]/>
	<#assign value = entry['value'][0]/>
	<#-- Saving value into root activity's cache using name as key -->
	<#assign void = activity.getRootActivity().getCache().add(name, value)/>
	<@LOG.log level="INFO" message="Add the entry ${name}"/>
	<item name="${name}">${value}</item>
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
<?xml version="1.0" encoding="UTF-8"?>
<root>
<#assign entries=Fn.getEntries(resources, 'entries', '') />
<#if entries?has_content>
	<#list entries as entry>
		<@WRITE_ENTRY_STORE_TO_CACHE entry=entry/>
	</#list>
<#else>
	<@LOG.log level="ERROR" message="Failed to read CSV entries"/>
	<#assign void = activity.setTrafficLight(trafficLight.RED)/>
</#if>
</root>