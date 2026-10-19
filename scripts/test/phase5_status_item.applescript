use scripting additions

-- CC5-S2D: read only the retained process's own status item for a human target cue.
on boundedText(rawValue)
    if rawValue is missing value then return "[unavailable]"
    set textValue to rawValue as text
    set AppleScript's text item delimiters to {tab, return, linefeed, character id 8232, character id 8233}
    set pieces to text items of textValue
    set AppleScript's text item delimiters to " "
    set cleanValue to pieces as text
    set AppleScript's text item delimiters to ""
    if (count characters of cleanValue) > 256 then set cleanValue to (text 1 thru 244 of cleanValue) & " [truncated]"
    return cleanValue
end boundedText

on geometryText(rawValues, fieldName, requirePositive)
    if (class of rawValues) is not list then error fieldName & " must contain two numeric values"
    if (count rawValues) is not 2 then error fieldName & " must contain two numeric values"
    repeat with rawValue in rawValues
        set numericValue to contents of rawValue
        set valueClass to class of numericValue
        if valueClass is not integer and valueClass is not real then error fieldName & " must contain two numeric values"
        if requirePositive then
            if numericValue <= 0 then error fieldName & " values must be positive"
        end if
    end repeat
    return my boundedText(((item 1 of rawValues) as text) & "," & ((item 2 of rawValues) as text))
end geometryText

on run argv
    if (count argv) is not 1 then error "Expected exactly one positive decimal owned PID"
    set pidText to item 1 of argv
    if pidText is "" then error "Positive PID required"
    repeat with digitText in characters of pidText
        if (contents of digitText) is not in "0123456789" then error "Decimal PID required"
    end repeat
    set ownedPid to pidText as integer
    if ownedPid < 1 then error "Positive PID required"
    with timeout of 12 seconds
        tell application "System Events"
            set ownedProcesses to application processes whose unix id is ownedPid
            if (count ownedProcesses) is not 1 then error "Exactly one retained PID process required"
            set ownedProcess to item 1 of ownedProcesses
            set extrasBar to value of attribute "AXExtrasMenuBar" of ownedProcess
            if extrasBar is missing value then error "Owned AXExtrasMenuBar unavailable"
            if (role of extrasBar) is not "AXMenuBar" then error "Owned extras role differs from AXMenuBar"
            if (count menu bar items of extrasBar) is not 1 then error "Exactly one owned extras item required"
            set extrasItem to menu bar item 1 of extrasBar
            set roleText to my boundedText(role of extrasItem)
            set nameText to "[unavailable]"
            try
                set nameText to my boundedText(name of extrasItem)
            end try
            set descriptionText to "[unavailable]"
            try
                set descriptionText to my boundedText(description of extrasItem)
            end try
            set positionText to my geometryText(position of extrasItem, "position", false)
            set sizeText to my geometryText(size of extrasItem, "size", true)
            set visibleText to "[unavailable]"
            try
                set visibleText to my boundedText(value of attribute "AXVisible" of extrasItem)
            end try
        end tell
        return "CC5-STATUS-ITEM" & linefeed & "pid=" & ownedPid & linefeed & "menuBarRole=AXMenuBar" & linefeed & "role=" & roleText & linefeed & "name=" & nameText & linefeed & "description=" & descriptionText & linefeed & "position=" & positionText & linefeed & "size=" & sizeText & linefeed & "visible=" & visibleText
    end timeout
end run
