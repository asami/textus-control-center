use framework "AppKit"
use scripting additions

-- CC5-S2D: observations of one retained PID, never production UI acceptance.
property nodeCount : 0
property reportText : ""
property reportTruncated : false

on boundedText(rawValue)
    if rawValue is missing value then return "[unavailable]"
    set textValue to rawValue as text
    set AppleScript's text item delimiters to {tab, return, linefeed}
    set pieces to text items of textValue
    set AppleScript's text item delimiters to " "
    set cleanValue to pieces as text
    set AppleScript's text item delimiters to ""
    if (count characters of cleanValue) > 256 then set cleanValue to (text 1 thru 244 of cleanValue) & " [truncated]"
    return cleanValue
end boundedText

on appendReport(rowText)
    if reportTruncated then return
    set reportText to reportText & linefeed & rowText
    if (count characters of reportText) > 24000 then
        set suffixText to linefeed & "[report truncated]"
        set reportText to (text 1 thru (24000 - (count characters of suffixText)) of reportText) & suffixText
        set reportTruncated to true
    end if
end appendReport

on activationEvidence(ownedPid)
    set nativeApplication to current application's NSRunningApplication's runningApplicationWithProcessIdentifier:ownedPid
    if nativeApplication is missing value then return "[unavailable: NSRunningApplication]"
    set nativePid to nativeApplication's processIdentifier() as integer
    if nativePid is not ownedPid then error "NSRunningApplication PID differs"
    return "pid=" & nativePid & "; active=" & (nativeApplication's isActive() as boolean) & "; policy=" & (nativeApplication's activationPolicy() as integer) & "; finishedLaunching=" & (nativeApplication's isFinishedLaunching() as boolean)
end activationEvidence

on windowEvidence(ownedProcess)
    set totalText to "[unavailable]"
    set visibleText to "[unavailable]"
    tell application "System Events"
        try
            set totalText to (count windows of ownedProcess) as text
        on error errorText
            set totalText to "[unavailable: " & my boundedText(errorText) & "]"
        end try
        try
            set visibleText to (count (windows of ownedProcess whose visible is true)) as text
        on error errorText
            set visibleText to "[unavailable: " & my boundedText(errorText) & "]"
        end try
    end tell
    return "total=" & totalText & "; visible=" & visibleText
end windowEvidence

on walkOwned(elementRef, depth)
    if reportTruncated then return
    if nodeCount >= 64 then
        my appendReport("[node bound truncated]")
        return
    end if
    set nodeCount to nodeCount + 1
    set nodeIndex to nodeCount
    set roleText to "[unavailable]"
    set nameText to "[unavailable]"
    set valueText to "[unavailable]"
    set childCount to missing value
    tell application "System Events"
        try
            set roleText to my boundedText(role of elementRef)
        end try
        try
            set nameText to my boundedText(name of elementRef)
        end try
        try
            set valueText to my boundedText(value of elementRef)
        end try
        try
            set childCount to count UI elements of elementRef
        end try
    end tell
    my appendReport("node=" & nodeIndex & "; depth=" & depth & "; role=" & roleText & "; name=" & nameText & "; value=" & valueText)
    if childCount is missing value then
        my appendReport("[unavailable: child count at node " & nodeIndex & "]")
        return
    end if
    if depth >= 6 then
        if childCount > 0 then my appendReport("[depth bound truncated at node " & nodeIndex & "]")
        return
    end if
    if childCount > 8 then my appendReport("[children truncated to first 8 at node " & nodeIndex & "]")
    set inspectedCount to childCount
    if inspectedCount > 8 then set inspectedCount to 8
    repeat with childIndex from 1 to inspectedCount
        if nodeCount >= 64 or reportTruncated then
            my appendReport("[node/report bound truncated]")
            exit repeat
        end if
        try
            tell application "System Events" to set childRef to UI element childIndex of elementRef
            my walkOwned(childRef, depth + 1)
        on error errorText
            my appendReport("[unavailable: child " & childIndex & ": " & my boundedText(errorText) & "]")
        end try
    end repeat
end walkOwned

on run argv
    set observeOnly to false
    if (count argv) is 2 then
        if (item 2 of argv) is not "--observe-only" then error "Expected exact --observe-only option"
        set observeOnly to true
    else if (count argv) is not 1 then
        error "Expected positive owned PID with optional --observe-only"
    end if
    set pidText to item 1 of argv
    if pidText is "" then error "Positive PID required"
    repeat with digitText in characters of pidText
        if (contents of digitText) is not in "0123456789" then error "Decimal PID required"
    end repeat
    set ownedPid to pidText as integer
    if ownedPid < 1 then error "Positive PID required"
    set nodeCount to 0
    set reportTruncated to false
    set reportText to "CC5-PRESENTATION-OBSERVED" & linefeed & "Diagnostic observations only; AX hierarchy alone proves no visual presence/absence or root cause."
    if observeOnly then my appendReport("Read-only observation after a human-click opportunity; no physical click or visual success is confirmed.")
    with timeout of 12 seconds
        tell application "System Events"
            set ownedProcesses to application processes whose unix id is ownedPid
            if (count ownedProcesses) is not 1 then error "Exactly one retained PID process required"
            set ownedProcess to item 1 of ownedProcesses
            my appendReport("before-SEfrontmost=" & (frontmost of ownedProcess))
        end tell
        try
            my appendReport("before-native=" & my activationEvidence(ownedPid))
        on error errorText
            my appendReport("before-native=[unavailable: " & my boundedText(errorText) & "]")
        end try
        if not observeOnly then
            tell application "System Events" to set frontmost of ownedProcess to true
            set frontmostDeadline to (current date) + 2
            tell application "System Events" to set afterFrontmost to frontmost of ownedProcess
            repeat while afterFrontmost is not true and (current date) < frontmostDeadline
                delay 0.1
                tell application "System Events" to set afterFrontmost to frontmost of ownedProcess
            end repeat
        else
            tell application "System Events" to set afterFrontmost to frontmost of ownedProcess
        end if
        my appendReport("after-SEfrontmost=" & afterFrontmost)
        try
            my appendReport("after-native=" & my activationEvidence(ownedPid))
        on error errorText
            my appendReport("after-native=[unavailable: " & my boundedText(errorText) & "]")
        end try
        my appendReport("before-windows=" & my windowEvidence(ownedProcess))
        if observeOnly then
            my appendReport("after-windows=" & my windowEvidence(ownedProcess))
            set ownedWindowCount to missing value
            try
                tell application "System Events" to set ownedWindowCount to count windows of ownedProcess
            on error errorText
                my appendReport("[unavailable: owned window count: " & my boundedText(errorText) & "]")
            end try
            if ownedWindowCount is not missing value then
                if ownedWindowCount > 2 then my appendReport("[owned windows truncated to first 2]")
                set inspectedWindowCount to ownedWindowCount
                if inspectedWindowCount > 2 then set inspectedWindowCount to 2
                repeat with windowIndex from 1 to inspectedWindowCount
                    if nodeCount >= 64 or reportTruncated then
                        my appendReport("[node/report bound truncated before owned window]")
                        exit repeat
                    end if
                    try
                        tell application "System Events" to set ownedWindow to window windowIndex of ownedProcess
                        my appendReport("owned-window=" & windowIndex)
                        my walkOwned(ownedWindow, 0)
                    on error errorText
                        my appendReport("[unavailable: owned window " & windowIndex & ": " & my boundedText(errorText) & "]")
                    end try
                end repeat
            end if
        else
            tell application "System Events"
                set extrasBar to value of attribute "AXExtrasMenuBar" of ownedProcess
                if extrasBar is missing value then error "Owned AXExtrasMenuBar unavailable"
                if (role of extrasBar) is not "AXMenuBar" then error "Owned extras role differs from AXMenuBar"
                if (count menu bar items of extrasBar) is not 1 then error "Exactly one owned extras item required"
                set extrasItem to menu bar item 1 of extrasBar
                set actionCount to count actions of extrasItem
                if actionCount > 16 then error "Owned action count exceeds 16"
                set hasPress to false
                repeat with actionIndex from 1 to actionCount
                    set actionName to name of action actionIndex of extrasItem
                    my appendReport("advertised-action=" & my boundedText(actionName))
                    if actionName is "AXPress" then set hasPress to true
                end repeat
                if not hasPress then error "Owned item does not advertise AXPress"
                perform action "AXPress" of extrasItem
            end tell
            my appendReport("AXPress dispatched exactly once")
            set windowDeadline to (current date) + 5
            set lastWindowEvidence to "[unavailable: no poll completed]"
            repeat while (current date) < windowDeadline
                set lastWindowEvidence to my windowEvidence(ownedProcess)
                delay 0.1
            end repeat
            my appendReport("after-windows=" & lastWindowEvidence)
        end if
        my walkOwned(ownedProcess, 0)
        return reportText
    end timeout
end run
