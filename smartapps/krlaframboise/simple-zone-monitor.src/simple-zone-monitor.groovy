/**
 *  Simple Zone Monitor v0.0.8 [ALPHA]
 *
 *  Author: 
 *    Kevin LaFramboise (krlaframboise)
 *
 *  URL to documentation:
 *
 *  Changelog:
 *
 *    0.0.8 (09/23/2016)
 *      - Added Button Arming/Disarming Triggers
 *      - Added All/Any/First option to Presence Arm/Disarm
 * 			  Triggers.
 *
 *    0.0.7 (09/16/2016)
 *      - CoRE resends piston state every 3 hours so added
 *        check to ignore events older than 60 seconds.
 *      - Moved Excluded Devices into Monitoring Status Zones
 *        section since arming zones and excluding devices
 *        go together.
 *      - Added icons for the activity section.
 *
 *    0.0.6 (09/15/2016)
 *      - Some code cleanup
 *      - Implemented Activity Viewing
 *
 *    0.0.5 (09/11/2016)
 *      - Added "Level" as option for Switch On Notifications.
 *      - Added additional information to Notifications page.
 *      - Added Custom Messages settings page that allows you
 *        to customize the Event Message and the Default Zone
 *        Message using tokens.
 *      - Rearranged Zone Settings page so that the custom
 *        messages for safety and security are shown together
 *        and only shows them for the selected device types.
 *      - Added token list and fixed the zone message explanation *        so that it matches the notification page terminology.
 *
 *    0.0.4 (09/10/2016)
 *      - Implemented Monitor Status Beep Confirmation
 *      - Implemented Entry/Exit Delay Beeping
 *        (requires device with beep() command)
 *      - Added backup handler that runs every minute and
 *        performs any scheduled tasks that are overdue.
 *
 *    0.0.3 (09/09/2016)
 *      - Implemented Entry/Exit Delay feature.
 *
 *    0.0.2 (09/08/2016)
 *      - Added CoRE Pistons as Arming/Disarming trigger.
 *      - Changed safety monitoring so that it's unrelated
 *        to zones being armed.
 *      - Bug fix for SHM arm/disarm trigger.
 *      - Other UI enhancements.
 *
 *    0.0.1 (09/04/2016)
 *      - Has basic safety/security monitoring and notifications.
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of
 *  the License at:
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in
 *  writing, software distributed under the License is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 *  OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
definition(
    name: "Simple Zone Monitor",
    namespace: "krlaframboise",
    author: "Kevin LaFramboise",
    description: "Monitors safety/security devices and performs actions when they're triggered.",
    category: "Safety & Security",
		iconUrl: "https://raw.githubusercontent.com/krlaframboise/Resources/master/simple-zone-monitor/app-SimpleZoneMonitor.png",
    iconX2Url: "https://raw.githubusercontent.com/krlaframboise/Resources/master/simple-zone-monitor/app-SimpleZoneMonitor@2x.png")

 preferences {
	page(name:"mainPage", title: "Simple Zone Monitor")
	page(name:"settingsPage", title: "Settings")
	page(name:"changeStatusPage", title: "Change Monitoring Status")
	page(name:"clearActivityPage")
	page(name:"activityDetailsPage")
	page(name:"statusesPage", title: "Monitoring Statuses")
  page(name:"devicesPage", title: "Devices")
	page(name:"zoneGroupsPage", title: "Zone Groups")
	page(name:"editZoneGroupPage", title: "Zone Group Details")
	page(name:"zonesPage", title: "Zones")
	page(name:"editZonePage", title: "Zone Details")
	page(name:"statusZonesPage", title: "Monitoring Status Zones")
	page(name:"editStatusZonesPage", title: "Monitoring Status Zones")
	//page(name:"refreshZonesPage", title: "Refresh Zones")
	page(name:"safetyNotificationsPage", title: "Safety Notifications")
	page(name:"securityNotificationsPage", title: "Security Notifications")
	page(name:"statusNotificationsPage", title: "Monitoring Status Notifications")
	page(name:"customMessagesPage", title: "Custom Messages")
	page(name:"armDisarmPage", title: "Arming/Disarming Options")
	page(name:"statusArmDisarmPage")
	page(name:"advancedOptionsPage", title: "Advanced Options")
	page(name:"statusAdvancedOptionsPage")	
}

def mainPage() {
	dynamicPage(name:"mainPage", uninstall:true, install:true) {		
		if (state.installed) {					
			def config = getConfigSummary()
			if (config.hasAllRequirements) {
				section("Change Monitoring Status") {
					getStatuses(true, true).each {
						if (it.name != state.status) {
							def imageName = (it.name == state.status?.name) ? "selected.png" : "unselected.png"
							getPageLink("${it.id}ChangeStatusLink",
								"${it.name}",
								"changeStatusPage",
								[status: it],
								"",
								"$imageName")
						}
					}
				}

				def armedZones = ""
				getZones(false)?.sort{ it.displayName }?.each {
					if (it.armed) {
						armedZones += "${it.displayName}\n"
					}
				}
				if (armedZones) {
					section("Armed Zones") {
						getParagraph(armedZones ?: "None", "armed.png")
					}
				}
				section("Activity") {
					getActivityContent()
				}				
			}
			else {
				section() {
					getWarningParagraph("Unable to monitor zones because there are unconfigured settings.")
				}
			}
			section("Settings") {
				getPageLink("settingsPageLink",
					"Settings",
					"settingsPage",
					null,
					config.hasAllRequirements ? "" : "(unconfigured)",
					"settings.png")
			}
		}
		else {
			section() {
				state.installed = true
				getParagraph("Installation Complete.\n\nPlease tap Done, exit the Marketplace, and open Simple Zone Monitor from your installed SmartApp list.", "app-SimpleZoneMonitor@2x.png")
			}
		}
	}
}

private getActivityContent() {
	state.securityAlerts = state.securityAlerts ?: []
	state.safetyAlerts = state.safetyAlerts ?: []
	state.statusHistory = state.statusHistory ?: []
	
	if (state.safetyAlerts) {	
		getPageLink("safetyEventsPageLink",
			"Safety Events",
			"activityDetailsPage",
			[activityType:"Safety Alerts"],
			getActivitySummary(state.safetyAlerts),
			"safety-alert.png")
	}
	if (state.securityAlerts) {
		getPageLink("securityEventsPageLink",
			"Security Events",
			"activityDetailsPage",
			[activityType:"Security Alerts"],
			getActivitySummary(state.securityAlerts),
			"security-alert.png")
	}
	if (state.statusHistory) {
		getPageLink("statusHistoryPageLink",
			"Monitoring Status History (${state.statusHistory.size()})",
			"activityDetailsPage",
			[activityType: "Monitoring Status History"],
			"",
			"status-history.png")
	}
	if (state.safetyAlerts || state.securityAlerts || state.statusHistory) {		
		getPageLink("clearActivityLink",
			"Clear All Activity",
			"clearActivityPage",
			null,
			"",
			"remove.png")
	}
	else {
		getParagraph("No Activity")
	}
}

private getActivitySummary(alerts) {
	def desc = ""
	def zones = []
	
	alerts?.each { alert ->		
		def zone = zones?.find { it.name == alert.zoneName }		
		if (zone) {			
			zone.events = (zone.events + 1)
		}
		else {
			zones << [name: alert.zoneName, events: 1]
		}
	}
	zones?.sort { it.name }?.each {
		desc += desc ? "\n" : ""
		desc += "${it.name} (${it.events})"
	}
	return desc
}

def activityDetailsPage(params) {
	dynamicPage(name:"activityDetailsPage") {
		def activityType = params?.activityType ?: ""
		def activity 
		switch (activityType) {
			case "Safety Alerts":
				activity = state.safetyAlerts
				break
			case "Security Alerts":
				activity = state.securityAlerts
				break
			case "Monitoring Status History":
				activity = state.statusHistory
				break
		}
			
		section("${activityType}") {
			if (activity) {				
				activity.each {
					if (activityType == "Monitoring Status History") {
						getStatusActivityParagraph(it)
					}
					else {
						getAlertActivityParagraph(it)
					}					
				}
				getPageLink("clearActivityLink",
					"Clear ${activityType}",
					"clearActivityPage",
					[activityType:"${activityType}"],					
					"",
					"remove.png")
			}
			else {
				getParagraph("No Activity")
			}
		}
	}
}

private getAlertActivityParagraph(activity) {	
	getParagraph("${getFormattedLocalTime(activity.eventTime)}\n${activity.status}\n${activity.deviceName}: ${activity.eventValue?.capitalize()}", "", "${activity.zoneName}")	
}

private getStatusActivityParagraph(activity) {
	getParagraph("${getFormattedLocalTime(activity.statusChanged)}\n${activity?.details}", "", "${activity.status?.name}")
}

def getFormattedLocalTime(utcDateString) {	
	def localTZ = TimeZone.getTimeZone(location.timeZone.ID)
	def utcTime = getDateFromUtcString(utcDateString).time
	def localDate = new Date(utcTime + localTZ.getOffset(utcTime))	
	return localDate.format("MM/dd/yyyy hh:mm:ss a")
}

private getDateFromUtcString(utcDateString) {
	return Date.parse("yyyy-MM-dd'T'HH:mm:ss", utcDateString.replace("+00:00", ""))
}

def clearActivityPage(params) {
	dynamicPage(name:"clearActivityPage") {
		def activityType = params?.activityType
		if (!activityType || activityType == "Safety Alerts") {
			state.safetyAlerts = []
			if (settings.allowAskAlexaSafetyMsgDeletion) {
				removeAskAlexaMsgs("Safety")
			}			
		}
		if (!activityType || activityType == "Security Alerts") {
			state.securityAlerts = []
			if (settings.allowAskAlexaSecurityMsgDeletion) {
				removeAskAlexaMsgs("Security")
			}			
		}
		if (!activityType || activityType == "Monitoring Status History") {
			state.statusHistory = []
		}		
		section() {
			getParagraph("${activityType ?: 'All Activity'} Cleared Successfully", "success.png")
		}
	}
}

private removeAskAlexaMsgs(unit) {
   sendLocationEvent(name: "AskAlexaMsgQueueDelete", value: "Simple Zone Monitor", isStateChange: true, unit: "$unit")
}

def settingsPage() {
	dynamicPage(name:"settingsPage") {
		def config = getConfigSummary()
		def unconfiguredDesc = "(unconfigured) "
		section("Master Settings") {
			getPageLink("statusLink",
				"Choose Monitoring Statuses",
				"statusesPage",
				null,
				config.hasStatuses ? "" : unconfiguredDesc)
			getPageLink("devicesLink",
				"Choose Devices",
				"devicesPage",
				null,
				config.hasSafetyOrSecurityDevices ? "" : unconfiguredDesc)
		}
		section("Zone Settings") {
			if (config.hasSafetyOrSecurityDevices) {
				getPageLink("zoneGroupsLink",
					"Zone Groups",
					"zoneGroupsPage")
				getPageLink("zonesLink",
					"Zones",
					"zonesPage",
					null,
					config.hasZones ? "" : unconfiguredDesc)
				if (config.hasZones && config.hasStatuses) {
					getPageLink("statusZonesLink",
						"Monitoring Status Zones",
						"statusZonesPage",
						null,
						config.hasStatusZones ? "" : unconfiguredDesc)
				}
				else {
					getWarningParagraph("Monitoring Status Zones can't be setup until Monitoring Statuses have been selected and \"Zones\" have been created.")
				}
			}
			else {
				getWarningParagraph("Zones can't be setup until at least one \"Safety Device to Monitor\" or \"Security Device to Monitor\" has been chosen.")
			}
		}
		section("Notification Settings") {
			if (config.hasStatuses) {
				getPageLink("safetyNotificationsLink",
					"Safety Notifications",
					"safetyNotificationsPage",
					null,
					config.hasConfiguredSafetyNotifications ? "" : (config.hasConfiguredSecurityNotifications ? "(not set)" : "(unconfigured)"))
				getPageLink("securityNotificationsLink",
					"Security Notifications",
					"securityNotificationsPage",
					null,
					config.hasConfiguredSecurityNotifications ? "" : (config.hasConfiguredSafetyNotifications ? "(not set)" : "(unconfigured)"))
				getPageLink("customMessagesLink",
					"Custom Messages",
					"customMessagesPage",
					null,
					"")				
			}
			else {
				getWarningParagraph("Notifications can't be configured until there's at least one \"Active Monitoring Status\" has been chosen.")
			}
		}
		section("Arming/Disarming and Advanced Options") {
			if (config.hasAllRequirements) {
				getPageLink("armDisarmLink",
					"Arming/Disarming",
					"armDisarmPage")
				getPageLink("advancedOptionsLink",
					"Entry/Exit Delays & Beeping",
					"advancedOptionsPage")
			}
			else {
				getWarningParagraph("Arming/Disarming and Advanced Options can't be setup until \"Zones\" and \"Notifications\" have been configured.")
			}
		}
		section("Ask Alexa Options") {
			getInfoParagraph("If you enable the options below, clearing the messages within this SmartApp will also clear them from the Ask Alexa SmartApp")
			input "allowAskAlexaSafetyMsgDeletion", "bool",
				title: "Allow deleting of Alexa Alexa Safety Messages?",
				defaultValue: false,
				required: false
			input "allowAskAlexaSecurityMsgDeletion", "bool",
				title: "Allow deleting of Alexa Alexa Security Messages?",
				defaultValue: false,
				required: false
		}
		section("Other Options") {	
			input "logging", "enum",
				title: "Log these types of messages to log:",
				multiple: true,
				required: false,
				defaultValue: ["debug", "info"],
				options: ["debug", "info", "trace"]
		}
	}
}

def getConfigSummary() {
	def config = [:]

	config.hasSafetyDevices = hasDevices(getSafetyDeviceTypes())
	config.hasSecurityDevices = hasDevices(getSecurityDeviceTypes())
	config.hasZones = hasZones()
	config.hasStatuses = hasStatuses()
	config.hasStatusZones = hasStatusZones()
	
	config.hasSafetyOrSecurityDevices = (config.hasSafetyDevices || config.hasSecurityDevices)

	config.hasConfiguredSafetyNotifications = hasConfiguredNotifications("Safety", null)

	config.hasConfiguredSecurityNotifications = hasConfiguredNotifications("Security", null)

	config.hasConfiguredSafetyOrSecurityNotifications = config.hasConfiguredSafetyNotifications || config.hasConfiguredSecurityNotifications

	config.hasAllRequirements = config.hasSafetyOrSecurityDevices && config.hasConfiguredSafetyOrSecurityNotifications && config.hasStatuses && config.hasZones && config.hasStatusZones && config.hasStatuses

	state.configSummary = config
	return config
}

def changeStatusPage(params) {
	dynamicPage(name:"changeStatusPage") {
		section() {
			changeStatus(params.status)
			addStatusHistory(params?.status, "Manual")
			getParagraph("Monitoring Status Changed to ${state.status?.name}", "success.png")
		}
	}
}

private addStatusHistory(status, details) {
	logDebug("${status?.name}: ${details}")
	state.statusHistory.add(0, [statusChanged: new Date(), status: status, details: details])
}

private changeStatus(newStatus) {
	if (newStatus) {
		state.delayedEvents = []
		state.entryEventTime = null
		state.beepStatus = null
		logInfo("Changing Monitoring Status to ${newStatus?.name}")
		state.status = newStatus
		state.status.time = new Date().time
		initialize()
		playConfirmationBeep()
		initializeEntryExitBeeping()
	}
	else {
		log.warn "Ignoring changeStatus($newStatus)"
	}	
}

private playConfirmationBeep() {
	findDevices(getNotificationDeviceTypes(), settings["${state.status.id}ConfirmationBeepDevices"])*.beep()	
}

private initializeEntryExitBeeping() {
	if (getCurrentEntryExitDelay() && getCurrentEntryExitBeepDeviceNames()) {
		logTrace "Starting entry/exit beeping"
		state.beepStatus = state.status
		playEntryExitBeep()
	}
	else {
		state.beepStatus = null
	}	
}

def entryExitBeepHandler(evt) {
	def beepFrequency = getCurrentEntryExitBeepFrequency()
	if (state.beepStatus && beepFrequency) {
		runIn(beepFrequency, playEntryExitBeep)
	}
}

def playEntryExitBeep() {
	def startTime = state.entryEventTime ?: state.status?.time
	
	if (state.beepStatus?.id == state.status.id && !timeElapsed(startTime, getCurrentEntryExitDelay())) {
		logTrace("Executing entry/exit beep on ${getCurrentEntryExitBeepDeviceNames()}")
		
		findDevices(getNotificationDeviceTypes(), getCurrentEntryExitBeepDeviceNames())*.beep()
		
		sendLocationEvent(name: "Simple Zone Monitor", value: "Entry/Exit Beep", isStateChange: true)	
	}
	else {
		state.beepStatus = null
	}
}

private int getCurrentEntryExitDelay() {
	return safeToInt(settings["${state.status?.id}EntryExitDelay"], 0)	
}

private int getCurrentEntryExitBeepFrequency() {
	return safeToInt(settings["${state.status.id}EntryExitBeepFrequency"], 0)
}

private int safeToInt(value, int defaultValue) {
	if (value && value instanceof Integer) {
		return (int)value		
	}
	else {
		return defaultValue
	}
}

private long safeToLong(value, defaultValue) {
	if (value && (value instanceof Long || value instanceof Integer)) {
		return (long)value		
	}
	else {
		return (long)safeToInt(value, defaultValue)
	}
}

private getCurrentEntryExitBeepDeviceNames() {
	return settings["${state.status.id}EntryExitBeepDevices"]
}

def statusesPage() {
	dynamicPage(name:"statusesPage") {
		section() {
			getInfoParagraph("Determines which zones are armed and which notifications to use when an Intrusion or Safety Alert occurs.  The relationships between the Monitoring Statuses and the Armed Zones can be configured from the \"Monitoring Status Zones\" screen.", "What is a Monitoring Status?")
		}
		section("Choose Your Monitoring Statuses") {
			input "selectedStatuses", "enum",
					title: "Only the Monitoring Statuses you select below will be visible throughout the rest of the SmartApp.",
					multiple: true,
					required: true,
					options: getOptionalStatusNames()
		}
		section("Required Monitoring Statuses") {
			getInfoParagraph("Disarmed and Disabled are Monitoring Statuses used throughout the SmartApp, but they're not listed above because they provide additional functionality and can't be hidden.")
			getInfoParagraph("When the Monitoring Status is set to Disarmed the Security Devices won't be monitored, but notifications can still be performed for the Safety Devices.", "Disarmed Monitoring Status")
			getInfoParagraph("When the Monitoring Status is set to Disabled, the Security Devices and Safety Devices won't be monitored.  The Arm/Disarm Triggers are also disabled so the only way to change it to a different Status is manually through the SmartApp.", "Disabled Monitoring Status")
		}
	}
}

def devicesPage() {
	dynamicPage(name:"devicesPage") {
		section("Safety Devices to Monitor") {
			getInfoParagraph("Safety devices that support multiple capabilities may appear in multiple Safety device fields, but you only need to select them once.")
			getSafetyDeviceTypes().each {
				input "${it.prefName}", "${it.prefType}",
					title: "${it.name}:",
					multiple: true,
					required: false
			}
		}
		section("Security Devices to Monitor") {
			getInfoParagraph("Security devices that support multiple capabilities may appear in multiple Security device fields, but you only need to select them once.")
			getSecurityDeviceTypes().each {
				input "${it.prefName}", "${it.prefType}",
					title: "${it.name}:",
					multiple: true,
					required: false
			}
		}
		section("Arming/Disarming Trigger Devices") {
			getInfoParagraph("Arming/Disarming devices that support multiple capabilities may appear in multiple Arming/Disarming device fields, but you only need to select them once.")
			getArmDisarmDeviceTypes().each {
				input "${it.prefName}", "${it.prefType}",
					title: "${it.name}:",
					multiple: true,
					required: false
			}
		}
		section("Notification Devices") {
			getInfoParagraph("Notification devices that support multiple capabilities may appear in multiple Notification device fields, but you only need to select them once.")
			getNotificationDeviceTypes().each {
				input "${it.prefName}", "${it.prefType}",
					title: "${it.name}:",
					multiple: true,
					required: false
			}
		}
		section("SMS Phone Numbers") {
			getSMSNotificationTypes().each {
				input "${it.prefName}", "${it.prefType}", 
					title: "${it.name}:",
					required: false
			}
		}
	}
}

def zoneGroupsPage() {
	dynamicPage(name:"zoneGroupsPage") {
		section() {
			getPageLink("addZoneGroupLink",
				"Add New Zone Group",
				"editZoneGroupPage",
				[zoneGroup: null])
			getZoneGroups().each {
				getPageLink("${it.settingName}Link",
					"${it.name}",
					"editZoneGroupPage",
					[zoneGroup: it],
					getZoneGroupSummary(it))
			}
		}
	}
}  

private getZoneGroupSummary(zoneGroup) {
	def summary = ""
	getZones(false)?.sort { it.name }?.each { zone ->
		if (zone?.zoneGroupName == zoneGroup.name) {
			summary += summary ? "\n" : ""
			summary += "${zone.name}"
		}
	}	
	return summary ?: "  > (No Zones)"
}

def editZoneGroupPage(params) {
	dynamicPage(name:"editZoneGroupPage") {
		section("Edit Zone Group") {
			def zoneGroup = params?.zoneGroup ?: getFirstEmptyZoneGroup()
			if (zoneGroup) {
				input "${zoneGroup.settingName}", "text",
					title: "Zone Group Name:",
					required: false
				if (zoneGroup.name) {
					paragraph "You can remove this Zone Group by clearing the Zone Group Name field."
				}
			}
			else {
				getWarningParagraph("You can only have 25 Zone Groups")
			}
		}
	}
}

def zonesPage() {
	dynamicPage(name:"zonesPage") {
		def hasZones = false

		section() {
			getPageLink("addZoneLink",
				"Add New Zone",
				"editZonePage",
				[zone: getFirstEmptyZone()])
			getZones(false).each {
				hasZones = true
				getPageLink("${it.settingName}Link",
					"${it.displayName}",
					"editZonePage",
					[zone: it],
					getZoneSummary(it))
			}
		}

		// if (hasZones) {
			// section("Zone Changes") {
				// paragraph "Zone changes that effect the Armed state won't get applied until you exit the application.  To apply these changes immediately, tap the Refresh Zones button below."
				// getPageLink("$refreshZonesLink",
					// "Refresh Zones",
					// "refreshZonesPage")
			// }
		// }
	}
}

private getZoneSummary(zone) {
	def summary = ""	
	if (zone?.status) {
		summary += summary ? "\n" : ""
		summary = "Zone Status: ${zone?.status}"
	}
	settings["${zone.settingName}SafetyDevices"]?.each {
		summary += summary ? "\n" : ""
		summary += "Safety: ${it}"
	}
	settings["${zone.settingName}SecurityDevices"]?.each {
		summary += summary ? "\n" : ""
		summary += "Security: ${it}"
	}	
	return summary ?: "(not set)"
}

// def refreshZonesPage(params) {
	// dynamicPage(name:"refreshZonesPage") {
		// section() {
			// initialize()
			// paragraph "Zones refreshed successfully"
		// }
	// }
// }

def editZonePage(params) {
	dynamicPage(name:"editZonePage") {
		if (params?.zone) {
			state.params.zone = params.zone
		}
		else if (!state.params.zone) {
			state.params.zone = getFirstEmptyZone()
		}
		def zone = state.params.zone
				
		if (zone) {
			section() {
				input "${zone.settingName}", "text",
					title: "Zone Name:",
					required: false
				input "${zone.settingName}Group", "enum",
					title: "Zone Group:",
					required: false,
					options: getZoneGroupNames()
			}
			section("Security Settings") {
				getInfoParagraph("Select the Security devices that should be monitored when this zone is armed.  If you need more control over which devices in a zone get armed for a specific Monitoring Status, you can use the 'Excluded Devices' field on the Monitoring Status Zones page.")
				
				input "${zone.settingName}SecurityDevices", "enum",
					title: "Security Devices:",
					multiple: true,
					required: false,
					submitOnChange: true,
					options: getDeviceNames(getSecurityDeviceTypes())
				
				if (settings["${zone.settingName}SecurityDevices"]) {
					getInfoParagraph("To reduce false alarms you can optionally require an event from more than one of the zone's devices within a specified amount of time.\n\n(NOT IMPLEMENTED)") 
					input "${zone.settingName}MultiEventSeconds", "number",
						title: "Require multiple events within: (seconds)",
						required: false
				}
			}
			section("Safety Settings") {
				getInfoParagraph("Safety devices are monitored regardless of the Zone's Armed Status.")
				input "${zone.settingName}SafetyDevices", "enum",
					title: "Safety Devices:",
					multiple: true,
					required: false,
					submitOnChange: true,
					options: getDeviceNames(getSafetyDeviceTypes())
			}
			section("Zone Messages") {
				getInfoParagraph("You can optionally specify a Zone Message for each type of device being monitored.  The Safety/Security Notifications page has message and audio settings that support using the Zone Message.", "What are Zone Messages?")
				getInfoParagraph("If you have an audio device that can play audio messages by track number, like the Aeon Labs Doorbell, you can use the Zone Message fields to assign different tracks for each type of device in each zone.  You can then use the \"Play Zone Message as Track\" Notification option to play the corresponding track when a safety or security event occurs.", "Other uses for Zone Messages")
				getInfoParagraph("If you leave the Zone Message fields empty and use one of the Zone Message Notification options, it will use the Default Zone Message which is located in the \"Custom Messages\" section.", "Zone Message Defaults")
				getTokensParagraph()
				def zoneMsgPrefs = []			
				 getSecurityDeviceTypes().each { deviceType ->
					def attr = deviceType.alarmAttr
					if (getDevices([deviceType]).find { it.hasAttribute(attr) && it.displayName in settings["${zone.settingName}SecurityDevices"]}) {
						zoneMsgPrefs << [name: "${zone.settingName}${deviceType.prefName}Message", title: "${deviceType.name} Zone Message:"]
					}
				}
				getSafetyDeviceTypes().each { deviceType ->
					def attr = deviceType.alarmAttr
					if (getDevices([deviceType]).find { it.hasAttribute(attr) && it.displayName in settings["${zone.settingName}SafetyDevices"]}) {
						zoneMsgPrefs << [name: "${zone.settingName}${deviceType.prefName}Message", title: "${deviceType.name} Zone Message:"]
					}
				}
				if (zoneMsgPrefs) {
					zoneMsgPrefs?.sort { it.title }?.each { pref ->
						input "${pref.name}", "text",
								title: "${pref.title}",
								required: false
					}
				}
				else {
					getWarningParagraph("The Zone Message fields are not available until you've selected at least one Safety Device or Security Device to monitor.")
				}
			}
			if (zone.name) {
				section() {
					paragraph "You can Delete this Zone by clearing the Zone Name field and tapping Done."
				}
			}
		}
		else {
			// It wasn't able to find an empty zone so display warning message.
			section() {
				getWarningParagraph("You can only have 100 Zones")
			}
		}
	}
}

def statusZonesPage() {
	dynamicPage(name:"statusZonesPage") {
		section() {
			getInfoParagraph("This section allows you to choose the Zones that should be armed for each Monitoringi Status.  It also allows you to select devices within those zones that shouldn't be armed.")
			getStatuses(false).each {
				getPageLink("${it.id}StatusZonesLink",
					"${it.name}",
					"editStatusZonesPage",
					[status: it],
					getStatusZonesSummary(it))
			}
		}
	}
}

private getStatusZonesSummary(status) {
	def zoneSummary = appendStatusSettingSummary("", status.id, "StatusZones", "%")
	
	def deviceSummary = appendStatusSettingSummary("", status.id, "ExcludedDevices", "%")
	
	def summary = ""		
	if (zoneSummary) {
		summary = "<---  Armed Zones  --->\n${zoneSummary}"
	}
	if (deviceSummary) {
		summary += summary ? "\n" : ""
		summary += "<---  Excluded Devices  --->\n${deviceSummary}"
	}
	return summary ?: "(not set)"	
}

def editStatusZonesPage(params) {
	dynamicPage(name:"editStatusZonesPage") {
		if (params?.status) {
			state.params.status = params.status
		}

		def id = state.params?.status?.id
		def name = state.params?.status?.name

		section("${name} - Zones") {
			getInfoParagraph("Select the Zones that should be armed while the Monitoring Status is ${name}.")
		}
		
		section("Zones") {
			input "${id}StatusZones", "enum",
				title: "Arm these Zones:",
				multiple: true,
				required: false,
				options: getZoneNames()
		}
		section("Excluded Devices") {
			input "${id}ExcludedDevices", "enum",
				title: "Don't Monitor these Devices:",
				multiple: true,
				required: false,
				options: getDeviceNames(getSecurityDeviceTypes()),
				submitOnChange: true
		}
	}
}

// def statusZonesPage() {
	// dynamicPage(name:"statusZonesPage") {
		// section() {
			// getInfoParagraph("Specify zones that are armed for the different Monitoring Statuses.")
			// getStatuses(false).each {
				// input "${it.id}StatusZones", "enum",
					// title: "${it.name}:",
					// multiple: true,
					// required: false,
					// options: getZoneNames()
			// }
		// }
	// }
// }

def safetyNotificationsPage() {
	dynamicPage(name:"safetyNotificationsPage") {
		section() {
			getInfoParagraph("Setup Safety Notifications for each Monitoring Status.")
			if (!state.configSummary?.hasSafetyDevices) {
				getWarningParagraph("These notifications won't get executed because no Safety Devices are being monitored.")
			}

			getStatuses(true).each {
				getPageLink("${it.id}SafetyNotificationsLink",
					"${it.name}",
					"statusNotificationsPage",
					[status: it, notificationType: "Safety"],
					getStatusNotificationsSummary("Safety", it?.id))
			}
		}
	}
} 

def securityNotificationsPage() {
	dynamicPage(name:"securityNotificationsPage") {
		section() {
			getInfoParagraph("Setup Security Notifications for each Monitoring Status.")
			if (!state.configSummary?.hasSecurityDevices) {
				getWarningParagraph("These notifications won't get executed because no Security Devices are being monitored.")
			}

			getStatuses(false).each {
				getPageLink("${it.id}SecurityNotificationsLink",
					"${it.name}",
					"statusNotificationsPage",
					[status: it, notificationType: "Security"],
					getStatusNotificationsSummary("Security", it?.id))
			}
		}
	}
} 

private getStatusNotificationsSummary(notificationType, statusId) {
	def summary = ""
	getStatusTypeSettings(getStatusNotificationTypeData(notificationType, statusId)).each {
		def settingValue = settings["${it.prefName}"]
		if (settingValue) {
			summary = summary ? "${summary}\n" : ""
			summary = "${summary}${it.prefTitle} ${settingValue}"
		}
	}				
	return summary ?: "(not set)"
}

def statusNotificationsPage(params) {
	dynamicPage(name:"statusNotificationsPage") {
		if (params?.status) {
			state.params.status = params.status
			state.params.notificationType = params.notificationType
		}

		getStatusTypeDataSections(getStatusNotificationTypeData(state.params?.notificationType, state.params?.status?.id))		
	}
}

private getNotificationNoDeviceMessage(fieldName, deviceType, cmd=null) {
	def supportsCmd = cmd ? ", that supports the \"$cmd\" command," : ""
	return "You can't use ${fieldName} until you select at least one \"${deviceType}\" device${supportsCmd} from the \"Notification Devices\" section of the \"Choose Devices\" page."
}

def customMessagesPage() {
	dynamicPage(name:"customMessagesPage") {
		section() {
			getTokensParagraph()			
			input "defaultZoneMessage", "text",
				title: "Default Zone Message:",
				defaultValue: getDefaultZoneMessage(),
				required: false
			input "defaultEventMessage", "text",
				title: "Default Event Message:",
				defaultValue: getEventMessage(),
				required: false
		}
	}
}

private getTokensParagraph() {
	def tokens = getMessageTokens().join("\n")
	getInfoParagraph("${tokens}", "The following tokens can be used in the messages:")
}

private getDefaultZoneMessage() {
	return settings.defaultZoneMessage ?: "%notificationType% event detected in Zone %zoneFullName% by %deviceName%"
}

private getEventMessage() {
	return settings.defaultEventMessage ?: "Zone %zoneFullName%: %deviceName% - %eventName% is %eventValue%"
}

def armDisarmPage() {
	dynamicPage(name:"armDisarmPage") {
		section() {
			getInfoParagraph("Specify triggers that activate the different Monitoring Statuses.")
			getStatuses(true).each {
				getPageLink("${it.id}ArmDisarmLink",
					"${it.name}",
					"statusArmDisarmPage",
					[status: it],
					getStatusArmDisarmSummary(it?.id))
			}
		}
	}
}

private getStatusArmDisarmSummary(statusId) {
	def summary = ""
	getStatusTypeSettings(getStatusArmDisarmTypeData(statusId)).each {
		def settingValue = settings["${it.prefName}"]
		def childPrefValue = getChildPrefValue(it.childPrefs, 0) ?: ""
		if (settingValue) {
			summary = summary ? "${summary}\n" : ""
			summary = "${summary}${it.prefTitle} ${childPrefValue} ${settingValue}"
		}
	}				
	return summary ?: "(not set)"	
}

def statusArmDisarmPage(params) {
	dynamicPage(name:"statusArmDisarmPage") {

		if (params?.status) {
			state.params.status = params.status
		}

		def id = state.params?.status?.id
		def name = state.params?.status?.name
			
		section("${name} - Arming/Disarming Options") {
			getInfoParagraph("Specify triggers that will cause the Monitoring Status to change to ${name}.", "${name}")
		}
		
		getStatusTypeDataSections(getStatusArmDisarmTypeData(id))	
	}
}

private getStatusTypeDataSections(data) {
	data?.each { sect ->
		section("${sect?.sectionName}") {			
			if (sect?.sectionInfo) {
				getInfoParagraph("${sect.sectionInfo}")
			}			
			sect?.subSections.each { subSect ->					
				if (!subSect?.noOptionsMsg || subSect?.options) {
					getStatusSubSectionSettings(subSect)?.each {
	
						input "${it.prefName}", "${it.prefType}", 
							title: "${it.prefTitle}",
							required: it.required,
							multiple: it.multiple,
							submitOnChange: it.submitOnChange,
							options: it.options
						
						it.childPrefs?.each { child ->
							input "${child.prefName}", "${child.prefType}",
								title: "${child.prefTitle}",
								required: child.required,
								multiple: child.multiple,
								options: child.options,
								range: child.range
						}
					}
				}
				else {
					paragraph "${subSect?.noOptionsMsg}"
				}			
			}
		}
	}
}

private getStatusArmDisarmTypeData(statusId) {
	def prefix = "${statusId}ArmDisarm"	
	[
		[sectionName: "Location Mode",
			sectionInfo: "",
			subSections: [
				[
					prefTitle: "When Mode changes to:",
					prefName: "${prefix}Modes",
					prefType: "mode",
					required: false,
					submitOnChange: false,
					multiple: true,
					options: null,
					noOptionsMsg: "",
					isDevice: false,
					prefs: [ 
						[name: "Location Mode", attrName: "mode"]
					],
					childPrefs: []
				]
			]
		],		
		[sectionName: "Smart Home Monitor",
			sectionInfo: "",
			subSections: [
				[
					prefTitle: "When Smart Home Monitor changes to",
					prefName: "${prefix}SmartHomeMonitorStates",
					prefType: "enum",
					required: false,
					submitOnChange: false,
					multiple: true,
					options: getSmartHomeMonitorStates().collect{ it.name },
					noOptionsMsg: "",
					isDevice: false,
					prefs: [ 
						[name: "Smart Home Monitor", attrName: "alarmSystemStatus"]
					],
					childPrefs: []
				]
			]
		],
		[sectionName: "CoRE Pistons",
			sectionInfo: "This SmartApp has limited arming/disarming features, but you can use the SmartApp CoRE to create complex rules and select them here to make the Monitoring Status change when they change to True." ,
			subSections: [
				[
					prefTitle: "When any of these CoRE Pistons change to true:",
					prefName: "${prefix}Pistons",
					prefType: "enum",
					required: false,
					submitOnChange: false,
					multiple: true,
					options: state.pistons,
					noOptionsMsg: "You need to have at least one Piston setup in the SmartApp CoRE in order to use this feature.  You may need to open and close CoRE in order to populate the piston list.",
					isDevice: false,
					subscribeToValues: true,
					prefs: [ 
						[name: "CoRE Piston", attrName: "piston"]
					],
					childPrefs: [ ]					
				]
			]
		],
		[sectionName: "Switches",
			subSections: [
				[
					prefTitle: "When any of these Switches Turn %:",
					prefName: "${prefix}Switches%",
					prefType: "enum",
					required: false,
					submitOnChange: false,
					multiple: true,
					options: getDeviceNames(getArmDisarmDeviceTypes(), null, "Switch"),
					noOptionsMsg: getArmDisarmNoDeviceMessage("Switches", "Switch"),
					isDevice: true,
					prefs: [
						[name: "Off", attrName: "switch", attrValue: "off"],
						[name: "On", attrName: "switch", attrValue: "on"]
					],
					childPrefs: [ ]						
				]
			]
		],
		[sectionName: "Buttons",
			subSections: [
				[
					prefTitle: "When any of these Buttons are %:",
					prefName: "${prefix}Buttons%",
					prefType: "enum",
					required: false,
					submitOnChange: true,
					multiple: true,
					options: getDeviceNames(getArmDisarmDeviceTypes(), null, "Button"),
					noOptionsMsg: getArmDisarmNoDeviceMessage("Buttons", "Button"),
					isDevice: true,
					prefs: [
						[name: "Pushed", attrName: "button", attrValue:"pushed"],
						[name: "Held", attrName: "button", attrValue: "held"]
					],
					childPrefs: [ 
						[prefTitle: "Button Number: (1-20)",
						prefName: "${prefix}ButtonNumber%",
						prefType: "number",
						required: true,
						multiple: false,
						range: "1..20",
						options: null]
					]
				]
			]
		],
		[sectionName: "Presence Sensors",
			subSections: [
				[
					prefTitle: "When Presence changes to %:",
					prefName: "${prefix}PresenceSensors%",
					prefType: "enum",
					required: false,
					submitOnChange: true,
					multiple: true,
					options: getDeviceNames(getArmDisarmDeviceTypes(), null, "Presence Sensor"),
					noOptionsMsg: getArmDisarmNoDeviceMessage("Presence", "Presence Sensor"),
					isDevice: true,
					prefs: [ 
						[name: "Present", attrName: "presence", attrValue: "present"],
						[name: "Not Present", attrName: "presence", attrValue: "not present"]
					],
					childPrefs: [
						[prefTitle: "Presence Condition:",
						prefName: "${prefix}PresenceSensorsCondition%",
						prefType: "enum",
						required: true,
						multiple: false,
						options: ["All", "Any", "First"]]
					]
				]
			]
		]
	]
}

def advancedOptionsPage() {
	dynamicPage(name:"advancedOptionsPage") {
		section() {
			getInfoParagraph("Configure Entry/Exit Delays and Beeping Options for each Monitoring Status.")
			getStatuses(true).each {				
				getPageLink("${it.id}advancedOptionsLink",
					"${it.name}",
					"statusAdvancedOptionsPage",
					[status: it],
					getStatusAdvancedOptionsSummary(it))
			}
		}
	}
}

private getStatusAdvancedOptionsSummary(status) {
	def summary = ""
	
	if (settings["${status.id}EntryExitDevices"]) {
		def entryExitDelay = settings["${status.id}EntryExitDelay"]
		
		summary = appendStatusSettingSummary(summary, status.id, "EntryExitDevices", "Delayed ${entryExitDelay}s: %")
			
		summary = appendStatusSettingSummary(summary, status.id, "EntryExitBeepDevices", "Beep during delay using %")
	
		summary = appendStatusSettingSummary(summary, status.id, "EntryExitBeepFrequency", "Beep Frequency: %s")		
	}
	
	summary = appendStatusSettingSummary(summary, status.id, "ConfirmationBeepDevices", "Play Confirmation Beep on %")
	
	return summary ?: "(not set)"
}

def statusAdvancedOptionsPage(params) {
	dynamicPage(name:"statusAdvancedOptionsPage") {
		if (params?.status) {
			state.params.status = params.status
		}
		def id = state.params?.status?.id
		def name = state.params?.status?.name
		def beepDeviceNames = getDeviceNames(getNotificationDeviceTypes(), "beep", null)
	
		section("${name} - Advanced Options") {
			getInfoParagraph("Configure Advanced Options for Monitoring Status ${name}.")
		}
		
		section("Entry/Exit Options") {
			input "${id}EntryExitDevices", "enum",
				title: "Use Delay for these Devices:",
				multiple: true,
				required: false,
				options: getDeviceNames(getSecurityDeviceTypes()),
				submitOnChange: true
			if (settings?."${id}EntryExitDevices") {
				input "${id}EntryExitDelay", "number",
					title: "Delay Length (seconds):",
					required: true
				if (beepDeviceNames) {
					input "${id}EntryExitBeepDevices", "enum",
						title: "Beep on these devices:",
						multiple: true,
						required: false,
						options: beepDeviceNames,
						submitOnChange: true
					if (settings["${id}EntryExitBeepDevices"]) {						
						input "${id}EntryExitBeepFrequency", "number",
							title: "Beep Frequency (seconds):",
							required: true
					}
				}
				else {
					getInfoParagraph("Entry/Exit Beeping can't be used because none of the selected Notification Devices support the 'beep' command")
				}
			}
		}
		
		section("Confirmation Beep") {
			if (beepDeviceNames) {			
				input "${id}ConfirmationBeepDevices", "enum",
					title: "Beep with these devices when Monitoring Status changes to ${name}:",
					multiple: true,
					required: false,
					options: beepDeviceNames
			}
			else {
				getInfoParagraph("Confirmation Beep can't be used because none of the selected Notification Devices support the 'beep' command")
			}
		}		
	}
}

private getInfoParagraph(txt, title="") {
	getParagraph(txt, "info.png", title)
}

private getWarningParagraph(txt, title="") {
	getParagraph(txt, "warning.png", title)
}

private getParagraph(txt, imageName="", title="") {
	if (imageName) {
		paragraph title: "$title", image: getImageUrl(imageName), "$txt"
	}
	else {
		paragraph title: "$title", "$txt"
	}
}

private getPageLink(linkName, linkText, pageName, args=null, description="", imageName="") {
	def map = [
		name: "$linkName", 
		title: "$linkText",
		description: "$description",
		page: "$pageName",
		required: false
	]
	if (args) {
		map.params = args
	}
	if (imageName) {
		map.image = getImageUrl(imageName)
	}
	else if (description == "(unconfigured)") {
		map.image = getImageUrl("warning.png")
	}
	href(map)
}

def installed() {
	state.status = [id: "disarm", name: "Disarmed"]
	initialize()
}

def updated() {	
	initialize()
	
	logDebug("State Used: ${(state.toString().length() / 100000)*100}%")
}

def initialize() {
	unsubscribe()
	unschedule()
	
	schedule("23 0/1 * * * ?", scheduledTaskBackupHandler)
	
	subscribe(location, "Simple Zone Monitor.Entry/Exit Beep", entryExitBeepHandler)
	subscribe(location, "CoRE", coreHandler)
	state.params = [:]
	state.delayedEvents = []
	state.entryEventTime = null
	armZones()
	if (state.status?.id != "disabled") {
		initializeMonitoredSecurityDevices()
		initializeMonitoredSafetyDevices()
		initializeArmDisarmTriggers()
	}
	else {
		logDebug "No devices are being monitored and the Monitoring Status change triggers are disabled because the current Monitoring Status is \"Disabled\"."
	}
}

def scheduledTaskBackupHandler() {
	if (state.pendingOff) {
		logTrace("Scheduled Task Backup: Executing turnOffDevice()")
		turnOffDevice()
	}
	
	if (state.entryEventTime && timeElapsed(state.entryEventTime, (getCurrentEntryExitDelay() + 10))) {
		logTrace("Scheduled Task Backup: Executing delayedSecurityEventHandler()")
		delayedSecurityEventHandler()
	}
}

def coreHandler(evt) {
	logTrace "Updating CoRE Piston List"
	state.pistons = evt.jsonData?.pistons
}

def armZones() {
	def statusZoneNames = getStatusSettings(state.status?.id, "StatusZones")
	
	logDebug("Arming/Disarming Zones for Monitoring Status ${state?.status?.name}")
	
	def details = "Zone Status:\n"
	getZones(false).sort { it.displayName }.each { zone ->
		if (state.status?.optional && zone?.name in statusZoneNames) {
			zone.armed = true
		}
		else {
			zone.armed = false
		}
		state."${zone.armedStateName}" = zone.armed
		details += "${zone.displayName}: ${zone.armed ? 'Armed' : 'Disarmed'}\n"		
	}
	logDebug(details)
}

private initializeArmDisarmTriggers() {
	def details = "Arming/Disarming Subscriptions:\n"
	def deviceTriggers = []
	def locationTriggers = []
	
	getStatuses().each { status ->
		
		getStatusTypeSettings(getStatusArmDisarmTypeData(status.id))?.each { sect ->		
			if (settings[sect.prefName]) {
				def values = sect.multiple ? settings[sect.prefName] : [settings[sect.prefName]]
				details += "${status.name} - ${sect.prefTitle} ${values}\n"
				
				if (sect.isDevice) {
					def trig = deviceTriggers.find { it.attrName == sect.attrName }
					if (trig) {
						trig.devices += values
					}
					else {
						deviceTriggers << [subscription: "${sect.attrName}.${sect.attrValue}", devices: values]
					}
				}
				else if (sect.subscribeToValues) {
					values.each { value ->
						def subscription = "${sect.attrName}.${value}"
						if (!(subscription in locationTriggers)) {
							locationTriggers << subscription
						}
					}
				}
				else {
					if (!(sect.attrName in locationTriggers)) {
						locationTriggers << sect.attrName
					}					
				}
			}
		}
	}
	
	locationTriggers.each {
		subscribe(location, "${it}", armDisarmLocationEventHandler)
	}
	
	deviceTriggers.each { trig ->		
		findDevices(getArmDisarmDeviceTypes(), trig.devices)?.each { device ->
			subscribe(device, trig.subscription, armDisarmDeviceEventHandler)
		}
	}
	logTrace(details)
}

def armDisarmLocationEventHandler(evt) {
	def item
	
	if (evt.name == "alarmSystemStatus") {
		def shmState = getSmartHomeMonitorStates().find { it.id == evt.value}?.name
		item = findArmDisarmLocationEventItem(evt.name, shmState)
	}
	else if (evt.name != "piston" || pistonEventIsValid(evt.data)) {
		item =  findArmDisarmLocationEventItem(evt.name, evt.value)
	}
	
	if (item) {		
		addStatusHistory(item.status, "${item?.sect?.sectionName} ${evt.value}")
		changeStatus(item.status)
	}
	else {
		logTrace "Ignoring location event ${evt.name}.${evt.value} because it's not an active Arm/Disarm Trigger"
	}
}

def findArmDisarmLocationEventItem(eventName, eventValue) {
	def item	
	getStatuses().find { status ->
		getStatusTypeSettings(getStatusArmDisarmTypeData(status.id))?.find { sect ->			
			if (sect.attrName == eventName && (!sect.attrValue ||  eventValue in settings[sect.prefName])) {				
				item = [status: status, sect: sect]
				return true
			}
		}
	}
	return item
}

private pistonEventIsValid(eventData) {
	if (eventData) {
		def slurper = new groovy.json.JsonSlurper()
		def data = slurper.parseText(eventData)			
		
		if (data?.state && data?.restricted != true) {			
			def utcDateString = data?.event?.event?.date
			if (utcDateString) {	
		
				def pistonTime = getDateFromUtcString(utcDateString)?.time
				if (pistonTime && pistonTime instanceof Long) {
					return ((new Date().time - pistonTime) < 60000)
				}
				else {
					logDebug "Unable to convert ${utcDateString} to local date"
				}		
			}
			else {
				logDebug "Unable to detect piston date: ${eventData}"
			}
		}		
	}
}

def armDisarmDeviceEventHandler(evt) {
	def matchingItems = findArmDisarmDeviceEventItems(evt.displayName, evt.name, evt.value)

	if (matchingItems) {
		
		def verifiedMatch =	matchingItems.find { match ->
			switch(evt.name) {
				case "button":
					return isArmDisarmButtonMatch(evt, match)
					break
				
				case "presence":
					return isArmDisarmPresenceMatch(evt, match)
					break
			
				default:
					addStatusHistory(verifiedMatch.status, "${evt.displayName}: ${evt.name} changed to ${evt.value}")
					return true
			}
		}
		if (verifiedMatch) {			
			changeStatus(verifiedMatch.status)
		}
	}
	else {
		logTrace "Ignoring ${evt.name}.${evt.value} event from ${evt.displayName} because it's not an active Arm/Disarm Trigger"
	}
}

def findArmDisarmDeviceEventItems(deviceName, eventName, eventValue) {
	def items = []
	getStatuses().each { status ->
	
		getStatusTypeSettings(getStatusArmDisarmTypeData(status.id))?.each { sect ->			
	
			if (sect.attrName == eventName && sect.attrValue == eventValue && deviceName in settings["${sect.prefName}"]) {
				items << [status: status, sect: sect]				
			}
			
		}		
	}
	return items
}

private isArmDisarmButtonMatch(evt, eventItem) {
	def eventBtn = evt.jsonData?.buttonNumber
	def expectedBtn = getChildPrefValue(eventItem.sect?.childPrefs, 0)	
	if (eventBtn && expectedBtn && eventBtn == expectedBtn) {
		addStatusHistory(eventItem.status, "${evt.displayName}: Button #${eventBtn} ${evt.value}")
			return true
	}
	else {
		logTrace "Ignoring ${evt.name} #${eventBtn} ${evt.value} event from ${evt.displayName} because it's not an active Arm/Disarm Trigger"
	}		
}

private isArmDisarmPresenceMatch(evt, eventItem) {
	def isMatch = true
	def presenceCondition = getChildPrefValue(eventItem.sect?.childPrefs, 0)	
	
	if (presenceCondition != "Any") {
		def nonMatchingPresence = presenceCondition == "First" ? evt.value : (evt.value == "present" ? "not present" : "present")
		
		def nonMatchingDevices = findDevices(getArmDisarmDeviceTypes(), settings["${eventItem.sect?.prefName}"])?.findAll { 
			device -> return (device.displayName != evt.displayName && device.currentPresence == nonMatchingPresence)}
		
		if (nonMatchingDevices) {
			isMatch = false
			logTrace "Ignoring ${evt.name}.${evt.value} event from ${evt.displayName} because the following devices are ${nonMatchingPresence}: ${nonMatchingDevices}"
		}			
	}
	
	if (isMatch) {
		addStatusHistory(eventItem.status, "${evt.displayName}: ${evt.name} changed to ${evt.value} (${presenceCondition})")
		return true
	}
}

private getStatusSettingName(statusId, partialSettingName, attrValue) {
	partialSettingName = partialSettingName?.capitalize() ?: ""
	
	if (attrValue != null) {
		partialSettingName = "${partialSettingName}${attrValue?.replace(' ', '')?.capitalize()}"
	}	
	return "${statusId}${partialSettingName}"
}

private appendStatusSettingSummary(summary, statusId, partialSettingName, summaryLineFormat) {	
	getStatusSettings(statusId, partialSettingName)?.each {
		summary += summary ? "\n" : ""
		summary += summaryLineFormat.replace("%", "${it}")
	}
	return summary
}

private getStatusSettings(statusId, partialSettingName) {
	def result = []
	partialSettingName = partialSettingName?.capitalize() ?: ""
		
	getStatuses().each { 
		if (!statusId || it.id == statusId) {
			def settingName = "${it.id}${partialSettingName}"
			if (settings[settingName]) {
				result += settings[settingName]
			}
		}
	}
	return result
}

private initializeMonitoredSafetyDevices() {
	def details = "Safety Device Subscriptions:\n"	
	getAllZoneSafetyDevices()?.sort { it.displayName }?.each { device ->
		getSafetyDeviceTypes().each { type ->
			if (device.hasAttribute("${type.alarmAttr}")) {				
				details += "${device.displayName}: ${type.alarmAttr?.capitalize()} Event\n"
				subscribe(device, "${type.alarmAttr}.${type.alarmValue}", "safetyEventHandler")
			}
		}
	}
	logTrace(details)
}

private getAllZoneSafetyDevices() {
	def devices = []
	def excludedDevices = settings["${state.status?.id}ExcludedDevices"]	
	getZones().each { zone ->			
		def zoneSafetyDevices = settings["${zone.settingName}SafetyDevices"]
		
		if (zoneSafetyDevices) {
			getDevices(getSafetyDeviceTypes()).each { device ->					
				if (device.displayName in zoneSafetyDevices) {
					devices << device
				}
			}
		}
	}
	return devices.unique()
}

private initializeMonitoredSecurityDevices() {
	def details = "Security Device Subscriptions:\n"
	getArmedSecurityDevices()?.sort { it.displayName }?.each { device ->
		getSecurityDeviceTypes().each { type ->
			if (device.hasAttribute("${type.alarmAttr}")) {				
				details += "${device.displayName}: ${type.alarmAttr?.capitalize()} Event\n"
				subscribe(device, "${type.alarmAttr}.${type.alarmValue}", "securityEventHandler")
			}
		}
	}
	logTrace(details)
}

private getArmedSecurityDevices() {
	def devices = []
	def excludedMsg = ""
	def excludedDevices = settings["${state.status?.id}ExcludedDevices"]
	
	getZones().each { zone ->
		if (zone.armed) {
			def zoneSecurityDevices = settings["${zone.settingName}SecurityDevices"]
			
			if (zoneSecurityDevices) {
					getDevices(getSecurityDeviceTypes()).each { device ->					
					if (device.displayName in zoneSecurityDevices) {					
						if (device.displayName in excludedDevices) {
							excludedMsg += "\n${device.displayName}"
						}
						else {
							devices << device
						}					
					}
				}
			}
		}
	}
	if (excludedMsg) {
		logTrace "The following devices are not being monitored because they're in the ${state.status?.name} Excluded Device List:${excludedMsg}"
	}
	return devices
}

// private getSecurityDevices() {
	// def devices = []
	// getSecurityDeviceTypes().each {
		// if (settings[it.prefName]) {
			// devices += settings[it.prefName]
		// }
	// }
	// return devices.unique()
// }

// private getAllSecurityDevices() {
	// def devices = []
	// getSecurityDeviceTypes().each { deviceType ->
		// def settingValue = settings[deviceType.prefName]
		// if (settingValue) {
			// if (deviceType.multiple) {
				// devices += settingValue
			// }
			// else {
				// devices << settingValue
			// }
		// }
	// }
	// return devices.flatten()
// }

def safetyEventHandler(evt) {
	logDebug("${evt.displayName}: ${evt.name?.capitalize()} is ${evt.value}")
	handleNotifications("Safety", evt)
}

def securityEventHandler(evt) {
	logDebug("${evt.displayName}: ${evt.name?.capitalize()} is ${evt.value}")
	
	if (evt.displayName in settings["${state.status?.id}EntryExitDevices"]) {
		handleEntryExitNotification(evt)
	}
	else {
		handleNotifications("Security", evt)
	}
}

private handleEntryExitNotification(evt) {
	int delaySeconds = getCurrentEntryExitDelay()
	long statusTime = safeToLong(state.status?.time, 0)
	
	if (delaySeconds > 0 && statusTime > 0) {
		if (!timeElapsed(statusTime, delaySeconds)) {
			logDebug("Ignoring security event from ${evt.displayName} because it's an entry/exit device and the Monitoring Status has changed within ${delaySeconds} seconds.")
		}
		else {
			state.delayedEvents << [name: evt.name, value: evt.value, displayName: evt.displayName]
			if (!state.entryEventTime) {
				
				logTrace("Delaying security event from ${evt.displayName} for ${delaySeconds} seconds because it's an entry/exit device.")
				
				state.entryEventTime = new Date().time			
				initializeEntryExitBeeping()
				runIn(delaySeconds, delayedSecurityEventHandler)
				
			}
			else {
				logTrace("Delaying security event from ${evt.displayName} because it's an entry/exit device.")				
			}			
		}
	}
	else {
		// Invalid delay time so status change time so handle normally.
		logDebug "${evt.displayName} is an entry/exit device, but handling it like a normal device because of invalid delay time or status change time.  (Entry Exit Delay: ${delaySeconds}, Monitoring Status Changed: ${statusTime})"
		handleNotifications("Security", evt)
	}
}

def delayedSecurityEventHandler() {
	if (timeElapsed(state.entryEventTime, getCurrentEntryExitDelay())) {
		state.delayedEvents?.each {
			handleNotifications("Security", it)
		}
		state.delayedEvents = []
		state.entryEventTime = null
		state.beepStatus = null
	}	
}

private timeElapsed(startTime, delaySeconds) {
	if (!startTime) {
		return true
	}
	else {
		return ((((new Date().time) - safeToLong(startTime, 0)) / 1000) >= safeToInt(delaySeconds, 0))
	}
}

private handleNotifications(notificationType, evt) {
	def currentZone = findZoneByDevice(notificationType, evt?.displayName)

	logInfo "$notificationType Event in Zone ${currentZone?.displayName}"

	def currentDeviceType = getDeviceType(notificationType, evt.name, evt.value)
	
	 def eventMsg = replaceMessageTokens(getEventMessage(), evt, currentZone, notificationType, currentDeviceType.name)

	def zoneMsg = replaceMessageTokens((settings["${currentZone?.settingName}${currentDeviceType?.prefName}Message"] ?: getDefaultZoneMessage()), evt, currentZone, notificationType, currentDeviceType.name)
	
	storeNotification(notificationType, currentZone, evt, eventMsg, zoneMsg)
	
	getStatusTypeSettings(getStatusNotificationTypeData(notificationType, state.status?.id)). each {
		def msg = it.prefName?.contains("Zone") ? zoneMsg : eventMsg
		
		if (settings["${it.prefName}"]) {			
			if (it.prefName?.contains("Push")) {				
				handlePushFeedNotification(it, msg)
			}
			else if (it.prefName?.contains("Sms")) {
				settings["${it.prefName}"]?.each { phone ->
					logTrace("Sending SMS message \"$msg\" to $phone")
					sendSmsMessage(phone, msg)
				}
			}
			else if (it.isDevice) {
				def devices = findDevices(getNotificationDeviceTypes(), settings["${it.prefName}"])
				if (devices) {
					if (it.prefName.contains("Speak")) {
						logTrace "Executing speak($msg) on: ${settings[it.prefName]}"
						devices*.speak(msg)
					}
					else if (it.prefName.contains("Play")) {
						playNotification(devices, it, msg)
					}
					else {
						logTrace "Executing ${it.cmd}() on : ${settings[it.prefName]}"
						devices*."${it.cmd}"()
						
						if (it.prefName.contains("Alarm")) {
							initializeAutoOff(it)
						}
						else if (it.prefName.contains("Switch")) {
							setSwitchLevel(devices, it)
						}
					}
				}
			}
		}
	}
}

private storeNotification(notificationType, zone, evt, eventMsg, zoneMsg) {
	def data = [
		zoneName: zone.displayName,
		deviceName: evt.displayName,
		eventName: evt.name,
		eventValue: evt.value,
		eventTime: new Date(),
		status: state.status?.name
	]
	if (notificationType == "Security") {
		state.securityAlerts.add(0, data)
	}
	else {		
		state.safetyAlerts.add(0, data)
	}
	sendLocationEvent(name: "SimpleDeviceViewerAlert", value: "${notificationType}", isStateChange: true, descriptionText: "${eventMsg}")	
}

private handlePushFeedNotification(notificationSetting, msg) {
	def options = settings["${notificationSetting.prefName}"]	
	def push = options?.find { it.contains("Push") }
	def displayOnFeed = options?.find { it.contains("Display") }
	def askAlexa = options?.find { it.contains("Alexa") }
	def askAlexaUnit = notificationSetting.prefName?.contains("Security") ? "Security" : "Safety"
	
	if (push && displayOnFeed) {
		logTrace("Sending Push & Displaying on Notification Feed Message: $msg")
		sendPush(msg)
	}
	else if (push) {
		logTrace("Sending Push Message: $msg")
		sendPushMessage(msg)
	}
	else if (displayOnFeed) {
		logTrace("Displaying on Notification Feed: $msg")
		sendNotificationEvent(msg)
	}
	if (askAlexa) {
		logTrace("Sending to Ask Alexa SmartApp: $msg")
		sendLocationEvent(name: "AskAlexaMsgQueue", value: "Simple Zone Monitor", isStateChange: true, descriptionText: "$msg", unit: "${askAlexaUnit}")
	}
}

private setSwitchLevel(devices, notificationSetting) {
	def value = getChildPrefValue(notificationSetting.childPrefs, 0)
	if (value && value instanceof Integer && value > 0 && value <= 100) {
		logTrace "Setting Level to ${value} for ${settings[notificationSetting.prefName]}"
		devices?.findAll { it.hasCommand("setLevel") }*.setLevel(value)
	}
}

private initializeAutoOff(notificationSetting) {
	def value = getChildPrefValue(notificationSetting.childPrefs, 0)
	if (value && value instanceof Integer) {
		if (!state.pendingOff) {
			state.pendingOff = []
		}
		int offSecs = (int)value
		long offTime = (new Date().time + (offSecs * 1000))
		
		state.pendingOff << [status: state.status, offTime: offTime, prefName: notificationSetting.prefName]
		
		logTrace "Scheduling devices to turn off in ${offSecs} seconds: ${settings[notificationSetting.prefName]}"
		runIn(offSecs, turnOffDevice, [overwrite: !canSchedule()])
	}
}

def turnOffDevice() {
	def pendingOff = []
	state.pendingOff?.each {
		if (it.status?.id == state.status?.id) {
			if (new Date().time > it.offTime) {
				logDebug "Turning Off: ${settings[it.prefName]}"
				findDevices(getNotificationDeviceTypes(), settings[it.prefName])*.off()
			}
			else {
				pendingOff << it
			}
		}
	}
	state.pendingOff = pendingOff
}

private playNotification(devices, notificationSetting, msg) {
	def volume = getChildPrefValue(notificationSetting.childPrefs, 0)
	if (!volume || !(volume instanceof Integer)) {
		volume = null
	}	
	logTrace "${notificationSetting.cmd}($msg, $volume)"
	devices*."${notificationSetting.cmd}"(msg, volume)
}

private getChildPrefValue(childPrefs, childIndex) {
	if (childPrefs && childPrefs.size() >= (childIndex-1)) {
		if (childPrefs[childIndex]?.prefName) {
			return settings[childPrefs[childIndex]?.prefName]
		}		
	}	
}

private findZoneByDevice(notificationType, deviceDisplayName) {
	getZones().find { zone ->
		if (zone.armed || notificationType == "Safety") {
			def zoneDeviceNames = settings["${zone.settingName}${notificationType}Devices"]
			return (deviceDisplayName in zoneDeviceNames)
		}
		else {
			return false
		}
	}
}

private getDeviceType(notificationType, eventName, eventVal) {
	def deviceTypes = (notificationType == "Security") ? getSecurityDeviceTypes() : getSafetyDeviceTypes()

	return deviceTypes.find {
		it.alarmAttr == eventName && it.alarmValue == eventVal
	}
}

def turnOffAlarm() {
	logTrace("Turning Off Alarms")
}

private getFirstEmptyZoneGroup() {
	def firstZoneGroup = null
	getZoneGroups(true).sort{ it.id }.each {
		if (!it.name && !firstZoneGroup) {
			firstZoneGroup = it
		}
	}
	return firstZoneGroup
}

private getZoneGroupNames() {
	getZoneGroups().collect { it.name }
}

private getZoneGroups(includeEmpty) {
	def zoneGroups = []
	for (int i = 0; i < 25; i++) {
		def zoneGroup = [id: i, settingName: "zoneGroup$i", name: settings["zoneGroup$i"] ?: ""]
		if (includeEmpty || zoneGroup.name) {
			zoneGroups << zoneGroup
		}
	}
	return zoneGroups.sort { it.name }
}

private getFirstEmptyZone() {
	def firstZone = null
	getZones(true).sort{ it.id }.each {
		if (!it.name && !firstZone) {
			firstZone = it
		}
	}
	return firstZone
}

private hasZones(includeEmpty=false) {
	return getZones(includeEmpty) ? true : false
}

private getZoneNames() {
	getZones().collect { it.name }
}

private getZones(includeEmpty=false) {
	def zones = []
	for (int i = 0; i < 100; i++) {

		def zone = [id: i, settingName: "zone$i", name: settings."zone$i" ?: ""]

		if (includeEmpty || zone.name) {
			zone.zoneGroupName = settings."${zone.settingName}Group" ?: ""
			zone.displayName = zone.zoneGroupName ? "${zone.zoneGroupName} > ${zone.name}" : "${zone.name}"

			zone.armedStateName = "${zone.settingName}Armed"
			zone.armed = state."${zone.armedStateName}" ?: false
			zone.status = zone.armed ? "Armed" : "Disarmed"

			zones << zone
		}
	}
	return zones.sort { it.displayName }
}

private getSafetyDeviceTypes() {
	return [
		[name: "Carbon Monoxide Detectors", shortName: "Carbon Monoxide", prefName: "safetyCarbonMonoxideDetector", prefType: "capability.carbonMonoxideDetector", alarmAttr: "carbonMonoxide", alarmValue: "detected"],
		[name: "Smoke Detectors", shortName: "Smoke", prefName: "safetySmokeDetector", prefType: "capability.smokeDetector", alarmAttr: "smoke", alarmValue: "detected"],
		[name: "Water Sensors", shortName: "Water", prefName: "safetyWaterSensors", prefType: "capability.waterSensor", alarmAttr: "water", alarmValue: "wet"]
	]
}

private getSecurityDeviceTypes() {
	return [
		[name: "Contact Sensors", shortName: "Contact", prefName: "securityContactSensors", prefType: "capability.contactSensor", alarmAttr: "contact", alarmValue: "open"],
		[name: "Motion Sensors", shortName: "Motion", prefName: "securityMotionSensors", prefType: "capability.motionSensor", alarmAttr: "motion", alarmValue: "active"]
	]
}

private getArmDisarmNoDeviceMessage(fieldName, deviceType) {
	return "You can't use ${fieldName} to Arm or Disarm until you select at least one \"${deviceType}\" from the \"Arm/Disarm Devices\" section of the \"Choose Devices\" page."
}

private getArmDisarmDeviceTypes() {
	return [
		[name: "Switches", shortName: "Switch", prefName: "armDisarmSwitches", prefType: "capability.switch", attrName: "switch", attrValues: ["on", "off"]],
		[name: "Buttons", shortName: "Button", prefName: "armDisarmButtons", prefType: "capability.button", attrName: "button", attrValues: ["pushed", "held"]],
		[name: "Presence Sensors", shortName: "PresenceSensor", prefName: "armDisarmPresenceSensors", prefType: "capability.presenceSensor", attrName: "presence", attrValues: ["present", "not present"]]
	]
}

private hasStatusZones() {
	return getStatusZoneNames(null) ? true : false
}

private getStatusZoneNames(statusId) {
	def names = []
	getStatuses().each { status ->
		if (!statusId || statusId == status?.id) {
			def statusZones = getStatusSettings(status?.id, "StatusZones")
			if (statusZones) {
				names += statusZones
			}
		}
	}
	return names
}

private hasStatuses() {
	return getStatuses() ? true : false
}

private hasConfiguredNotifications(notificationType, statusId) {
	return getStatusNotificationDeviceNames(notificationType, statusId) ? true : false
}

private getStatusNotificationDeviceNames(notificationType, statusId) {
	def names = []

	getStatuses().each { status ->
		if (!statusId || statusId == status?.id) {		
			getStatusTypeSettings(getStatusNotificationTypeData(notificationType, status?.id)).each {
				if (settings["${it.prefName}"]) {
					names += settings["${it.prefName}"]
				}
			}
		}
	}
	return names
}

private getStatusTypeSettings(data) {
	def result = []	
	data?.each { sect ->
		sect.subSections.each { subSect ->
			getStatusSubSectionSettings(subSect).each {
				it.sectionName = sect.sectionName
				result << it
			}
		}
	}	
	return result
}

// private getStatusNotificationSettings(notificationType, statusId) {
	// def result = []	
	// getStatusNotificationTypeData(notificationType, statusId).each { sect ->
		// sect.subSections.each { subSect ->
			// getStatusSubSectionSettings(subSect).each {
				// result << it
			// }
		// }
	// }	
	// return result
// }

private hasDevices(deviceTypes) {
	return getDevices(deviceTypes) ? true : false
}

private getDeviceNames(deviceTypes, cmd=null, capability=null) {
	def names = []
	getDevices(deviceTypes).each { 
		if ((!cmd || it.hasCommand(cmd)) && (!capability || it.hasCapability(capability))) {
			names << it.displayName
		}
	}
	return names.sort()
}

private findDevices(deviceTypes, deviceNameList) {
	def devices = []
	if (deviceNameList) {
		getDevices(deviceTypes).each { device ->
			if (device.displayName in deviceNameList) {
				devices << device
			}
		}
	}
	return devices
}

private getDevices(deviceTypes) {
	def devices = []
	deviceTypes?.each {
		if (settings[it.prefName]) {
			devices += settings[it.prefName]
		}
	}
	return devices.unique()
}

private getStatusSubSectionSettings(subSection) {
	def result = []
	
	if (subSection.prefs) {
		def childPrefs = subSection.childPrefs?.clone()
		subSection.prefs.each { pref ->
			def item = subSection.clone()
			item.prefName = "${item.prefName.replace('%', pref.name?.replace(' ', ''))}"
			item.prefTitle = "${item.prefTitle.replace('%', pref.name)}"
			item.cmd = "${pref.cmd}"
			item.attrName = "${pref.attrName}"
			item.attrValue = "${pref.attrValue}"
			
			if (settings["${item.prefName}"] && !pref.ignoreChildren) {
				item.childPrefs = []
				subSection.childPrefs?.each {
					def childPref = it.clone()
					childPref.prefName = "${it.prefName.replace('%', pref.name)}"
					childPref.prefTitle = "${it.prefTitle.replace('%', pref.name)}"
					item.childPrefs << childPref
				}
			}
			else {
				item.childPrefs = []
			}
			result << item
		}
	}	
	else {
		if (!settings["${subSection.prefName}"]) {
			subSection.childPrefs = null
		}
		result << subSection
	}		
	return result
}

private getStatusNotificationTypeData(notificationType, statusId) {
	def prefix = "${statusId}${notificationType}"	
	[
		[sectionName: "Push/Feed ${notificationType} Notifications",
			sectionInfo: getZoneVsEventMsg(),
			subSections: [
				[
					prefTitle: "Push/Feed % Notifications:",
					prefName: "${prefix}PushFeed%Msg",
					prefType: "enum",
					required: false,
					submitOnChange: false,
					multiple: true,
					options: ["Display on Notification Feed", "Push Message", "Send to Ask Alexa SmartApp"],
					noOptionsMsg: "",
					isDevice: false,
					prefs: [ 
						[name: "Zone"],
						[name: "Event"]
					],
					childPrefs: []
				]
			]
		],
		[sectionName: "SMS ${notificationType} Notifications",
			sectionInfo: msgTypeSectionInfo,
			subSections: [
				[
					prefTitle: "Send SMS with % Message to:",
					prefName: "${prefix}Sms%Msg",
					prefType: "enum",
					required: false,
					submitOnChange: false,
					multiple: true,
					options: getSMSNotificationPhoneNumbers(),
					noOptionsMsg: "You can't use SMS Notifications until you enter at least one \"SMS Phone Number\" into the \"SMS Phone Numbers\" section of the \"Choose Devices\" page.",
					isDevice: false,
					prefs: [ 
						[name: "Zone"],
						[name: "Event"]
					],
					childPrefs: []
				]
			]
		],
		[sectionName: "Alarm ${notificationType} Notifications",
			sectionInfo: getZoneVsEventMsg(),
			subSections: [
				[
					prefTitle: "Turn on %:",
					prefName: "${prefix}Alarm%",
					prefType: "enum",
					required: false,
					submitOnChange: true,
					multiple: true,
					options: getDeviceNames(getNotificationDeviceTypes(), null, "Alarm"),
					noOptionsMsg: getNotificationNoDeviceMessage("Alarm Notifications", "Alarm", null),
					isDevice: true,
					prefs: [ 
						[name: "Siren", cmd: "siren"],
						[name: "Strobe", cmd: "strobe"],
						[name: "Both", cmd: "both"]
					],
					childPrefs: [
						[prefTitle: "Turn % off after (seconds):",
						prefName: "${prefix}Alarm%Off",
						prefType: "number",
						required: false,
						multiple: false,
						options: null]
					]
				]
			]
		],
		[sectionName: "Switch ${notificationType} Notifications",
			subSections: [
				[
					prefTitle: "Turn %:",
					prefName: "${prefix}Switch%",
					prefType: "enum",
					required: false,
					submitOnChange: true,
					multiple: true,
					options: getDeviceNames(getNotificationDeviceTypes(), null, "Switch"),
					noOptionsMsg: getNotificationNoDeviceMessage("Switch Notifications", "Switch", null),
					isDevice: true,
					prefs: [ 
						[name: "On", cmd: "on"],
						[name: "Off", cmd: "off", ignoreChildren: true]
					],
					childPrefs: [
						[prefTitle: "Set Level: (1-100)",
						prefName: "${prefix}SwitchLevel",
						prefType: "number",
						required: false,
						multiple: false,
						range: "1..100",
						options: null]
					]
				]
			]
		],
		[sectionName: "Audio ${notificationType} Notifications",
			sectionInfo: "The Zone Message can be used to play messages on TTS devices, but you can also use it to play specific tracks on devices like the Aeon Labs Doorbell.",
			subSections: [
				[
					prefTitle: "Speak % Message on:",
					prefName: "${prefix}Speak%",
					prefType: "enum",
					required: false,
					submitOnChange: false,
					multiple: true,
					options: getDeviceNames(getNotificationDeviceTypes(), "speak", null),
					noOptionsMsg: getNotificationNoDeviceMessage("Speak Message", "Speech Synthesis", null),
					isDevice: true,
					prefs: [ 
						[name: "Zone", cmd: "speak"]
					],
					childPrefs: []
				],
				[
					prefTitle: "Play % Message as Text on:",
					prefName: "${prefix}Play%Text",
					prefType: "enum",
					required: false,
					submitOnChange: true,
					multiple: true,
					options: getDeviceNames(getNotificationDeviceTypes(), "playText", null),
					noOptionsMsg: getNotificationNoDeviceMessage("Play Message as Text", "Audio Notification or Music Player", "playText"),
					isDevice: true,
					prefs: [ 
						[name: "Zone", cmd: "playText"]
					],
					childPrefs: [
						[prefTitle: "Play Text Volume: (1-100)",
						prefName: "${prefix}Play%TextVolume",
						prefType: "number",
						required: false,
						multiple: false,
						range: "1..100",
						options: null]
					]
				],
				[
					prefTitle: "Play % Message as Track on:",
					prefName: "${prefix}Play%Track",
					prefType: "enum",
					required: false,
					submitOnChange: true,
					multiple: true,
					options: getDeviceNames(getNotificationDeviceTypes(), "playTrack", null),
					noOptionsMsg: getNotificationNoDeviceMessage("Play Message as Track", "Audio Notification or Music Player", "playTrack"),
					isDevice: true,
					prefs: [ 
						[name: "Zone", cmd: "playTrack"]
					],
					childPrefs: [
						[prefTitle: "Play Track Volume: (1-100)",
						prefName: "${prefix}Play%TrackVolume",
						prefType: "number",
						required: false,
						multiple: false,
						range: "1..100",
						options: null]
					]
				]
			]
		],
		[sectionName: "Photo ${notificationType} Notifications",
			subSections: [
				[
					prefTitle: "% Photo with:",
					prefName: "${prefix}%Photo",
					prefType: "enum",
					required: false,					
					submitOnChange: false,
					multiple: true,
					options: getDeviceNames(getNotificationDeviceTypes(), null, "Image Capture"),
					noOptionsMsg: getNotificationNoDeviceMessage("Photo Notifications", "Image Capture", null),
					isDevice: true,
					prefs: [ 
						[name: "Take", cmd: "take"]
					],
					childPrefs: []
				]
			]
		]
	]
}

private getZoneVsEventMsg() {
	return "In this section, you have the option to use either the \"Zone Message\" or \"Event Message\".  The Zone Message is the message specified in the active device's zone settings.  The Event Message is the message specified in the \"Default Messages\" Settings."
}

private getNotificationDeviceTypes() {
	return [
		[name: "Alarm", prefName: "notificationAlarms", prefType: "capability.alarm"],
		[name: "Audio Notification", prefName: "notificationAudioNotificationGenerators", prefType: "capability.audioNotification"],
		[name: "Image Capture", prefName: "notifictionImageCapture", prefType: "capability.imageCapture"],
		[name: "Music Player", prefName: "notificationMusicPlayers", prefType: "capability.musicPlayer"],
		[name: "Speech Synthesis", prefName: "notificationSpeechSynthesizers", prefType: "capability.speechSynthesis"],
		[name: "Switch", prefName: "notificationSwitches", prefType: "capability.switch"],
		[name: "Tone", prefName: "notificationToneGenerators", prefType: "capability.tone"]
	]
}

private getSMSNotificationPhoneNumbers() {
	def phoneNumbers = []
	getSMSNotificationTypes().each {
		if (settings[it.prefName]) {
			phoneNumbers += settings[it.prefName]
		}
	}
	return phoneNumbers.unique()
}

private getSMSNotificationTypes() {
	return [
		[name: "SMS Phone Number 1", prefName: "notificationPhone1", prefType: "phone"],
		[name: "SMS Phone Number 2", prefName: "notificationPhone2", prefType: "phone"],
		[name: "SMS Phone Number 3", prefName: "notificationPhone3", prefType: "phone"]
	]
}

private getStatusNames(includeDisarmed=true, includeDisabled=false) {
	return getStatuses(includeDisarmed, includeDisabled).collect { it.name }
}

private getOptionalStatusNames() {
	getAllStatuses().findAll { it.optional }?.collect { it.name }
}

private getStatuses(includeDisarmed=true, includeDisabled=false) {
	def statuses = []	
	getAllStatuses().each { status ->
		def isAllowed = ((includeDisarmed || status.id != "disarmed") && (includeDisabled || status.id != "disabled"))
		if (isAllowed && (!status.optional || status.name in settings.selectedStatuses)) {
			statuses << status
		}
	}
	return statuses
}

private getAllStatuses() {
	def items = [
		[id: "active", name: "Armed (Active)", optional: true],
		[id: "alone", name: "Armed (Alone)", optional: true],
		[id: "away", name: "Armed (Away)", optional: true],
		[id: "outdoors", name: "Armed (Outdoors)", optional: true],
		[id: "relaxed", name: "Armed (Relaxed)", optional: true],
		[id: "sleep", name: "Armed (Sleep)", optional: true],
		[id: "stay", name: "Armed (Stay)", optional: true],
		[id: "testing", name: "Armed (Testing)", optional: true],
		[id: "trusted", name: "Armed (Trusted Visitor)", optional: true],
		[id: "untrusted", name: "Armed (Untrusted Visitor)", optional: true],
		[id: "disabled", name: "Disabled", optional: false],
		[id: "disarmed", name: "Disarmed", optional: false]
	]
	return items
}

private getSmartHomeMonitorStates() {
	return [
		[id: "away", name: "Armed (away)"],
		[id: "stay", name: "Armed (stay)"],
		[id: "off", name: "Disarmed"]
	]
}

private replaceMessageTokens(msg, evt, zone, notificationType, deviceType) {
	return (msg ?: "")
		.replace("%eventName%", "${evt.name}")
		.replace("%eventValue%", "${evt.value}")
		.replace("%deviceName%", "${evt.displayName}")
		.replace("%zoneGroupName%", "${zone.zoneGroupName}")
		.replace("%zoneName%", "${zone.name}")
		.replace("%zoneFullName%", "${zone.displayName}")
		.replace("%notificationType%", "${notificationType}")
		.replace("%deviceType%", "${deviceType}")
}

private getMessageTokens() {
	[
		"%eventName%", 
		"%eventValue%",
		"%deviceName%",
		"%deviceType%",
		"%zoneGroupName%",
		"%zoneName%",
		"%zoneFullName%",
		"%notificationType%"
	].sort()
}

private getImageUrl(imageName) {
		return "https://raw.githubusercontent.com/krlaframboise/Resources/master/simple-zone-monitor/${imageName}"
}

private logDebug(msg) {
	if (loggingTypeEnabled("debug")) {
		log.debug msg
	}
}

private logTrace(msg) {
	if (loggingTypeEnabled("trace")) {
		log.trace msg
	}
}

private logInfo(msg) {
	if (loggingTypeEnabled("info")) {
		log.info msg
	}
}

private loggingTypeEnabled(loggingType) {
	return (!settings?.logging || settings?.logging?.contains(loggingType))
}