<#import "logging.macros.ftl" as LOG>

<#-- ****************************************************************************** -->
<#-- ********************************** MACROS ************************************ -->
<#-- ****************************************************************************** -->
<#macro WRITE_ENTRY_USING_CACHE entry>
	<#assign name = entry['name'][0]/>
	<#assign value = entry['value'][0]/>
	<#-- Restoring values from parent and root activities' caches using name as key -->
	<#assign parentCacheValues = activity.getParentActivity().getCache().get(name)/>
	<#assign rootCacheValues = activity.getRootActivity().getCache().get(name)/>
	<@LOG.log level="INFO" message="Add the entry ${name}"/>
	<item name="${name}"<#if parentCacheValues?has_content> parentCacheValues="${parentCacheValues?join(", ")}"</#if><#if rootCacheValues?has_content> rootCacheValues="${rootCacheValues?join(", ")}"</#if>>${value}</item>
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
<?xml version="1.0" encoding="UTF-8"?>
<root>
<#assign entries=Fn.getEntries(resources, 'entries', '') />
<#if entries?has_content>
	<#list entries as entry>
		<@WRITE_ENTRY_USING_CACHE entry=entry/>
	</#list>
<#else>
	<@LOG.log level="ERROR" message="Failed to read CSV entries"/>
	<#assign void = activity.setTrafficLight(trafficLight.RED)/>
</#if>
</root>