<#-- ****************************************************************************** -->
<#-- ********************************** MACROS ************************************ -->
<#-- ****************************************************************************** -->
<#macro log level message>
	<#assign void = Fn.log(level, message)>
</#macro>
<#macro ENTRY entry>
	<@log level="INFO" message="Add the entry ${entry['name'][0]}"/>
	<item name="${entry['name'][0]}">${entry['value'][0]}</item>
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
<?xml version="1.0" encoding="UTF-8"?>
<root>
<#assign entries=Fn.getEntries(resources, 'entries', '') />
<#if entries?has_content>
	<#list entries as entry>
		<@ENTRY entry=entry/>
	</#list>
<#else>
	<@log level="ERROR" message="Failed to CSV entries"/>
	<#assign void = activity.setTrafficLight(trafficLight.RED)/>
</#if>
</root>