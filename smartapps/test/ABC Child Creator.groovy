/*	
 *
 *	ABC Child Creator for Advanced Button Controller
 *
 *	Author: SmartThings, modified by Bruce Ravenel, Dale Coffing, Stephan Hackett
 * 
 *
 *  2/06/18 - converted code to hubitat format
 * 		removed ability to hide "held options"
 *		removed hwspecifics section as is no longer applicable
 *		adjusted device list to look for "capability.pushableButton"
 *		adjusted buttonDevice subscription (pushed, held, doubleTapped)
 *		adjusted buttonEvent() to swap "name" and "value" as per new rules
 * 2/08/18 - change formatting for Button Config Preview (Blue/Complete color)
 *		Added Double Tap inputs and edited shallHide() getDescription()
 *		added code for showDouble() to only display when controller support DT
 *		removed enableSpec and other Virtual Container Code as this is not supported in Hubitat
 *2/12/18
 * 		Updated to new detailsMap and modified Button Config/Preview pages
 *		hides secondary values if primary not set. When dispayed they are now "required". 
 *
 *2/12/18
 *		Switched to parent/child config	
 *		removed button pics and descriptive text (not utilized by hubitat)
 */
def version(){"v0.2.180213"}

definition(
    name: "ABC Child Creator",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Assign tasks to your Button Controller Devices",
    category: "My Apps",
    parent: "stephack:Advanced Button Controller",
    iconUrl: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
)

preferences {
	page(name: "chooseButton")
	page(name: "configButtonsPage")
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def chooseButton() {
	dynamicPage(name: "chooseButton", install: true, uninstall: true) {
		section("Step 1: Select Your Button Device") {
			input "buttonDevice", "capability.pushableButton", title: "Button Device", multiple: false, required: true, submitOnChange: true
		}
        if(buttonDevice){
        	state.buttonType =  buttonDevice.typeName
            if(state.buttonType.contains("Aeon Minimote")) state.buttonType =  "Aeon Minimote"
            log.debug "Device Type is now set to: "+state.buttonType
            state.buttonCount = manualCount?: buttonDevice.currentValue('numberOfButtons')
            //if(state.buttonCount==null) state.buttonCount = buttonDevice.currentValue('numButtons')	//added for kyse minimote(hopefully will be updated to correct attribute name)
            section("Step 2: Configure Your Buttons"){
            	if(state.buttonCount<1) {
                	paragraph "The selected button device did not report the number of buttons it has. Please specify in the Advanced Config section below."
                }
                else {
                	for(i in 1..state.buttonCount){
                		href "configButtonsPage", title: "Button ${i}", state: getDescription(i)!="Tap to configure"? "complete": null, description: getDescription(i), params: [pbutton: i]
                    }
            	}
            }
		}
        section("Set Custom Name (Optional)") {
        	label title: "Assign a name:", required: false
        }
        section("Advanced Config:", hideable: true, hidden: hideOptionsSection()) {
            	input "manualCount", "number", title: "Set/Override # of Buttons?", required: false, description: "Only set if DTH does not report", submitOnChange: true
                input "collapseAll", "bool", title: "Collapse Unconfigured Sections?", defaultValue: true
                //input "hwSpecifics", "bool", title: "Hide H/W Specific Details?", defaultValue: false
			}
        section(title: "Only Execute When:", hideable: true, hidden: hideOptionsSection()) {
			def timeLabel = timeIntervalLabel()
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
					options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
	}
}

def configButtonsPage(params) {
	if (params.pbutton != null) state.currentButton = params.pbutton.toInteger()
	dynamicPage(name: "configButtonsPage", title: "CONFIGURE BUTTON ${state.currentButton}:\n${state.buttonType}", getButtonSections(state.currentButton))
}

def getButtonSections(buttonNumber) {
	return {    	
        def myDetail
        for(i in 1..17) {//Build 1st 17 Button Config Options
        	myDetail = getPrefDetails().find{it.sOrder==i}
        	section(myDetail.secLabel, hideable: true, hidden: !(shallHide("${myDetail.id}${buttonNumber}") || shallHide("${myDetail.sub}${buttonNumber}"))) {
				input "${myDetail.id}${buttonNumber}_pushed", myDetail.cap, title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
				if(myDetail.sub && isReq("${myDetail.id}${buttonNumber}_pushed")) input "${myDetail.sub}${buttonNumber}_pushed", "number", title: myDetail.sTitle, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_pushed"), description: myDetail.sDesc
				//if(showHeld()) input "${myDetail.id}${buttonNumber}_held", myDetail.cap, title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
				input "${myDetail.id}${buttonNumber}_held", myDetail.cap, title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
                if(myDetail.sub && isReq("${myDetail.id}${buttonNumber}_held")) input "${myDetail.sub}${buttonNumber}_held", "number", title: myDetail.sTitle, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_held"), description: myDetail.sDesc
                if(showDouble()) input "${myDetail.id}${buttonNumber}_doubleTapped", myDetail.cap, title: "When Double Tapped", multiple: true, required: false, submitOnChange: collapseAll
                if(myDetail.sub && isReq("${myDetail.id}${buttonNumber}_doubleTapped")) input "${myDetail.sub}${buttonNumber}_doubleTapped", "number", title: myDetail.sTitle, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_held"), description: myDetail.sDesc
                
			}
        	if(i==3 || i==8 || i==13 || i==17) section(" "){}
        }        
		section("Set Mode", hideable: true, hidden: !shallHide("mode_${buttonNumber}")) {
			input "mode_${buttonNumber}_pushed", "mode", title: "When Pushed", required: false, submitOnChange: collapseAll
			input "mode_${buttonNumber}_held", "mode", title: "When Held", required: false, submitOnChange: collapseAll
            if(showDouble()) input "mode_${buttonNumber}_doubleTapped", "mode", title: "When Double Tapped", required: false, submitOnChange: collapseAll
		}
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
        	section("Run Routine", hideable: true, hidden: !shallHide("phrase_${buttonNumber}")) {
				//log.trace phrases
				input "phrase_${buttonNumber}_pushed", "enum", title: "When Pushed", required: false, options: phrases, submitOnChange: collapseAll
				input "phrase_${buttonNumber}_held", "enum", title: "When Held", required: false, options: phrases, submitOnChange: collapseAll
                if(showDouble()) input "phrase_${buttonNumber}_doubleTapped", "enum", title: "When Double Tapped", required: false, options: phrases, submitOnChange: collapseAll
			}
		}
        section("Notifications:\nSMS", hideable:true , hidden: !shallHide("notifications_${buttonNumber}")) {
        paragraph "****************\nPUSHED\n****************"
			input "notifications_${buttonNumber}_pushed", "text", title: "Message To Send When Pushed:", description: "Enter message to send", required: false, submitOnChange: collapseAll
            input "phone_${buttonNumber}_pushed","phone" ,title: "Send Text To:", description: "Enter phone number", required: false, submitOnChange: collapseAll
            //input "valNotify${buttonNumber}_pushed","bool" ,title: "Notify In App?", required: false, defaultValue: false, submitOnChange: collapseAll
        paragraph "\n\n*************\nHELD\n*************"
			input "notifications_${buttonNumber}_held", "text", title: "Message To Send When Held:", description: "Enter message to send", required: false, submitOnChange: collapseAll
			input "phone_${buttonNumber}_held", "phone", title: "Send Text To:", description: "Enter phone number", required: false, submitOnChange: collapseAll
			//input "valNotify${buttonNumber}_held", "bool", title: "Notify In App?", required: false, defaultValue: false, submitOnChange: collapseAll
            if(showDouble()) {
            	paragraph "\n\n*************************\nDOUBLE TAPPED\n*************************"
				input "notifications_${buttonNumber}_doubleTapped", "text", title: "Message To Send When Double Tapped:", description: "Enter message to send", required: false, submitOnChange: collapseAll
				input "phone_${buttonNumber}_doubleTapped", "phone", title: "Send Text To:", description: "Enter phone number", required: false, submitOnChange: collapseAll
				//input "valNotify${buttonNumber}_doubleTapped", "bool", title: "Notify In App?", required: false, defaultValue: false, submitOnChange: collapseAll           
            }
		}        
	}
}

def shallHide(myFeature) {
	if(collapseAll) return (settings["${myFeature}_pushed"]||settings["${myFeature}_held"]||settings["${myFeature}_doubleTapped"]||settings["${myFeature}"])
	return true
}

def isReq(myFeature) {
    (settings[myFeature])? true : false
}

def showHeld() {
	if(state.buttonType.contains("100+ ")) return false
	else return true
}

def showDouble() {
    def devCaps = buttonDevice.capabilities.find{it.name=="DoubleTapableButton"}
    if(devCaps) return true
	return false
}

def getDescription(dNumber) {
    def descript = ""
    if(!(settings.find{it.key.contains("_${dNumber}_")})) return "Tap to configure"
    if(settings.find{it.key.contains("_${dNumber}_pushed")}) descript = "\nPUSHED:"+getDescDetails(dNumber,"_pushed")+"\n"
    if(settings.find{it.key.contains("_${dNumber}_held")}) descript = descript+"\nHELD:"+getDescDetails(dNumber,"_held")+"\n"
    if(settings.find{it.key.contains("_${dNumber}_doubleTapped")}) descript = descript+"\nTAPx2:"+getDescDetails(dNumber,"_doubleTapped")+"\n"
    //if(anySettings) descript = "PUSHED:"+getDescDetails(dNumber,"_pushed")+"\n\nHELD:"+getDescDetails(dNumber,"_held")//"CONFIGURED : Tap to edit"
	return descript
}

def getDescDetails(bNum, type){
	def numType=bNum+type
	def preferenceNames = settings.findAll{it.key.contains("_${numType}")}.sort()		//get all configured settings that: match button# and type, AND are not false
    if(!preferenceNames){
    	return "  **Not Configured** "
    }
    else {
    	def formattedPage =""
    	preferenceNames.each {eachPref->
        	def prefDetail = getPrefDetails().find{eachPref.key.contains(it.id)}	//gets decription of action being performed(eg Turn On)
        	def prefDevice = " : ${eachPref.value}" - "[" - "]"											//name of device the action is being performed on (eg Bedroom Fan)
            	def prefSubValue = settings[prefDetail.sub + numType]?:"(!Missing!)"
            	if(prefDetail.type=="normal") formattedPage += "\n- ${prefDetail.desc}${prefDevice}"
            	if(prefDetail.type=="hasSub") formattedPage += "\n- ${prefDetail.desc}${prefSubValue}${prefDevice}"
            	if(prefDetail.type=="bool") formattedPage += "\n- ${prefDetail.desc}"
    	}
		return formattedPage
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    log.debug "INITIALIZED with settings: ${settings}"
	app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)
	subscribe(buttonDevice, "pushed", buttonEvent)
	subscribe(buttonDevice, "held", buttonEvent)
	subscribe(buttonDevice, "doubleTapped", buttonEvent)
    state.lastshadesUp = true
}

def defaultLabel() {
	return "${buttonDevice} Mapping"
}

def getPrefDetails(){
	def detailMappings =
    	[[id:'lightOn_', sOrder:1, desc:'Turn On ', comm:turnOn, type:"normal", secLabel: "Switches (Turn On)", cap: "capability.switch"],
         [id:'lights_', sOrder:3, desc:'Toggle On/Off', comm:toggle, type:"normal", secLabel: "Switches (Toggle On/Off)", cap: "capability.switch"],
     	 [id:"lightOff_", sOrder:2, desc:'Turn Off', comm:turnOff, type:"normal", secLabel: "Switches (Turn Off)", cap: "capability.switch"],
     	 [id:"lightDim_", sOrder:4, desc:'Dim to ', comm:turnDim, sub:"valLight", type:"hasSub", secLabel: "Dimmers (On to Level - Group 1)", cap: "capability.switchLevel", sTitle: "Bright Level", sDesc:"0 to 100%"],
     	 [id:"lightD2m_", sOrder:5, desc:'Dim to ', comm:turnDim, sub:"valLight2", type:"hasSub", secLabel: "Dimmers (On to Level - Group 2)", cap: "capability.switchLevel", sTitle: "Bright Level", sDesc:"0 to 100%"],
         [id:'dimPlus_', sOrder:6, desc:'Brightness +', comm:levelUp, sub:"valDimP", type:"hasSub", secLabel: "Dimmers (Increase Level By)", cap: "capability.switchLevel", sTitle: "Increase by", sDesc:"0 to 15"],
     	 [id:'dimMinus_', sOrder:7, desc:'Brightness -', comm:levelDown, sub:"valDimM", type:"hasSub", secLabel: "Dimmers (Decrease Level By)", cap: "capability.switchLevel", sTitle: "Decrease by", sDesc:"0 to 15"],
         [id:'lightsDT_', sOrder:8, desc:'Toggle Off/Dim to ', comm:dimToggle, sub:"valDT", type:"hasSub", secLabel: "Dimmers (Toggle OnToLevel/Off)", cap: "capability.switchLevel", sTitle: "Bright Level", sDesc:"0 to 100%"],
         [id:"speakerpp_", sOrder:9, desc:'Toggle Play/Pause', comm:speakerplaystate, type:"normal", secLabel: "Speakers (Toggle Play/Pause)", cap: "capability.musicPlayer"],
     	 [id:'speakervu_', sOrder:10, desc:'Volume +', comm:levelUp, sub:"valSpeakU", type:"hasSub", secLabel: "Speakers (Increase Vol By)", cap: "capability.musicPlayer", sTitle: "Increase by", sDesc:"0 to 15"],
     	 [id:"speakervd_", sOrder:11, desc:'Volume -', comm:levelDown, sub:"valSpeakD", type:"hasSub", secLabel: "Speakers (Decrease Vol By)", cap: "capability.musicPlayer", sTitle: "Decrease by", sDesc:"0 to 15"],
         [id:'speakernt_', sOrder:12, desc:'Next Track', comm:speakernexttrack, type:"normal", secLabel: "Speakers (Go to Next Track)", cap: "capability.musicPlayer"],
    	 [id:'speakermu_', sOrder:13, desc:'Mute', comm:speakermute, type:"normal", secLabel: "Speakers (Toggle Mute/Unmute)", cap: "capability.musicPlayer"],
         [id:'sirens_', sOrder:14, desc:'Toggle', comm:toggle, type:"normal", secLabel: "Sirens (Toggle)", cap: "capability.alarm"],
     	 [id:"locks_", sOrder:15, desc:'Lock', comm:setUnlock, type:"normal", secLabel: "Locks (Lock Only)", cap: "capability.lock"],
     	 [id:"fanAdjust_", sOrder:16,desc:'Adjust', comm:adjustFan, type:"normal", secLabel: "Fans (Adjust - Low, Medium, High, Off)", cap: "capability.switchLevel"],
     	 [id:"shadeAdjust_", sOrder:17,desc:'Adjust', comm:adjustShade, type:"normal", secLabel: "Shades (Adjust - Up, Down, or Stop)", cap: "capability.doorControl"],
     	 [id:"mode_", desc:'Set Mode', comm:changeMode, type:"normal"],
     	 [id:"phrase_", desc:'Run Routine', comm:runRout, type:"normal"],
		 [id:"notifications_", desc:'Send Push Notification', comm:messageHandle, sub:"valNotify", type:"bool"],
     	 [id:"phone_", desc:'Send SMS', comm:smsHandle, sub:"notifications_", type:"normal"],
         [id:"container_", desc:'Cycle Playlist', comm:cyclePL, type:"normal"],         
        ]
    return detailMappings
}

def buttonEvent(evt) {
	if(allOk) {
    	def buttonNumber = evt.value
		def pressType = evt.name
		log.debug "$buttonDevice: Button $buttonNumber was $pressType"
    	def preferenceNames = settings.findAll{it.key.contains("_${buttonNumber}_${pressType}")}
    	preferenceNames.each{eachPref->
        	def prefDetail = getPrefDetails()?.find{eachPref.key.contains(it.id)}		//returns the detail map of id,desc,comm,sub
        	def PrefSubValue = settings["${prefDetail.sub}${buttonNumber}_${pressType}"]	//value of subsetting (eg 100)
        	if(prefDetail.sub) "$prefDetail.comm"(eachPref.value,PrefSubValue)
        	else "$prefDetail.comm"(eachPref.value)
    	}
	}
}

def turnOn(devices) {
	log.debug "Turning On: $devices"
	devices.on()
}

def turnOff(devices) {
	log.debug "Turning Off: $devices"
	devices.off()
}

def turnDim(devices, level) {
	log.debug "Dimming (to $level): $devices"
	devices.setLevel(level)
}

def adjustFan(device) {
	log.debug "Adjusting: $device"
	def currentLevel = device.currentLevel
	if(device.currentSwitch == 'off') device.setLevel(15)
	else if (currentLevel < 34) device.setLevel(50)
  	else if (currentLevel < 67) device.setLevel(90)
	else device.off()
}

def adjustShade(device) {
	log.debug "Shades: $device = ${device.currentMotor} state.lastUP = $state.lastshadesUp"
	if(device.currentMotor in ["up","down"]) {
    	state.lastshadesUp = device.currentMotor == "up"
    	device.stop()
    } else {
    	state.lastshadesUp ? device.down() : device.up()
//    	if(state.lastshadesUp) device.down()
//        else device.up()
        state.lastshadesUp = !state.lastshadesUp
    }
}

def speakerplaystate(device) {
	log.debug "Toggling Play/Pause: $device"
	device.currentValue('status').contains('playing')? device.pause() : device.play()
}
   
def speakernexttrack(device) {
	log.debug "Next Track Sent to: $device"
	device.nextTrack()
}   

def speakermute(device) {
	log.debug "Toggling Mute/Unmute: $device"
	device.currentValue('mute').contains('unmuted')? device.mute() : device.unmute()
} 

def levelUp(device, inclevel) {
	log.debug "Incrementing Level (by +$inclevel: $device"
	def currentVol = device.currentLevel[0]//device.currentValue('level')[0]	//currentlevel return a list...[0] is first item in list ie volume level
    def newVol = currentVol + inclevel
  	device.setLevel(newVol)
    log.debug "Level increased by $inclevel to $newVol"
}

def levelDown(device, declevel) {
	log.debug "Decrementing Level (by -declevel: $device"
	def currentVol = device.currentLevel[0]//device.currentValue('level')[0]
    def newVol = currentVol.toInteger()-declevel
  	device.setLevel(newVol)
    log.debug "Level decreased by $declevel to $newVol"
}

def setUnlock(devices) {
	log.debug "Locking: $devices"
	devices.lock()
}

def toggle(devices) {
	log.debug "Toggling: $devices"
	if (devices*.currentValue('switch').contains('on')) {
		devices.off()
	}
	else if (devices*.currentValue('switch').contains('off')) {
		devices.on()
	}
	else if (devices*.currentValue('alarm').contains('off')) {
        devices.siren()
    }
	else {
		devices.on()
	}
}

def dimToggle(devices, dimLevel) {
	log.debug "Toggling On/Off | Dimming (to $dimLevel): $devices"
	if (devices*.currentValue('switch').contains('on')) devices.off()
	else devices.setLevel(dimLevel)
}

def runRout(rout){
	log.debug "Running: $rout"
	location.helloHome.execute(rout)
}

def messageHandle(msg, inApp) {
	if(inApp==true) {
    	log.debug "Push notification sent"
    	sendPush(msg)
	}
}

def smsHandle(phone, msg){
    log.debug "SMS sent"
    sendSms(phone, msg ?:"No custom text entered on: $app.label")
}

def changeMode(mode) {
	log.debug "Changing Mode to: $mode"
    log.debug mode[0]
    def tess = location.modes
    tess.each{momo->
        log.error momo.name
        if(momo.name == mode) log.info "found it"
    }
    log.error tess
	if (location.mode != mode && location.modes?.find { it.name == mode[0] }) setLocationMode(mode)
}

def cyclePL(device) {
	//int currPL = device.currentValue('lastRun')
   // int nextPL = currPL+1
    device.cycleChild()
    //device.on(nextPL)

}

// execution filter methods
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes || manualCount) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

private def textHelp() {
	def text =
	section("User's Guide - Advanced Button Controller") {
    	paragraph "This smartapp allows you to use a device with buttons including, but not limited to:\n\n  Aeon Labs Minimotes\n"+
    	"  HomeSeer HS-WD100+ switches**\n  HomeSeer HS-WS100+ switches\n  Lutron Picos***\n\n"+
		"It is a heavily modified version of @dalec's 'Button Controller Plus' which is in turn"+
        " a version of @bravenel's 'Button Controller+'."
   	}
	section("Some of the included changes are:"){
        paragraph "A complete revamp of the configuration flow. You can now tell at a glance, what has been configured for each button."+
        "The button configuration page has been collapsed by default for easier navigation."
        paragraph "The original apps were hardcoded to allow configuring 4 or 6 button devices."+
        " This app will automatically detect the number of buttons on your device or allow you to manually"+
        " specify (only needed if device does not report on its own)."
		paragraph "Allows you to give your buton device full speaker control including: Play/Pause, NextTrack, Mute, VolumeUp/Down."+
    	"(***Standard Pico remotes can be converted to Audio Picos)\n\nThe additional control options have been highlighted below."
	}
	section("Available Control Options are:"){
        paragraph "	Switches - Toggle \n"+
        "	Switches - Turn On \n"+
        "	Switches - Turn Off \n"+
        "	Dimmers - Toggle \n"+
        "	Dimmers - Set Level (Group 1) \n"+
        "	Dimmers - Set Level (Group 2) \n"+
        "	*Dimmers - Inc Level \n"+
        "	*Dimmers - Dec Level \n"+
        "	Fans - Low, Medium, High, Off \n"+
        "	Shades - Up, Down, or Stop \n"+
        "	Locks - Unlock Only \n"+
        "	Speaker - Play/Pause \n"+
        "	*Speaker - Next Track \n"+
        "	*Speaker - Mute/Unmute \n"+
        "	*Speaker - Volume Up \n"+
        "	*Speaker - Volume Down \n"+
        "	Set Modes \n"+
        "	Run Routines \n"+
        "	Sirens - Toggle \n"+
        "	Push Notifications \n"+
        "	SMS Notifications"
	}
	section ("** Quirk for HS-WD100+ on Button 5 & 6:"){
        paragraph "Because a dimmer switch already uses Press&Hold to manually set the dimming level"+
        " please be aware of this operational behavior. If you only want to manually change"+
        " the dim level to the lights that are wired to the switch, you will automatically"+
        " trigger the 5/6 button event as well. And the same is true in reverse. If you"+ 
        " only want to trigger a 5/6 button event action with Press&Hold, you will be manually"+
        " changing the dim level of the switch simultaneously as well.\n"+
        "This quirk doesn't exist of course with the HS-HS100+ since it is not a dimmer."
	}
	section("*** Lutron Pico Requirements:"){
        paragraph "Lutron Picos are not natively supported by SmartThings. A Lutron SmartBridge Pro, a device running @njschwartz's python script (or node.js) and the Lutron Caseta Service Manager"+
    	" SmartApp are also required for this functionality!\nSearch the forums for details."
	}
}
