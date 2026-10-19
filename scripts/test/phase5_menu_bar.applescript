use framework "AppKit"
use scripting additions

-- CC5-S2C: Accessibility is confined to the retained case-installed native PID.
property nodeCount : 0
property observedRows : {}
property semanticTexts : {}
property dashboardButtons : {}
property refreshButtons : {}
property quitButtons : {}
property hierarchyNodeCount : 0
property hierarchyReport : ""
property hierarchyReportTruncated : false
property traversalDiagnostics : false
property diagnosticBytes : 0
property diagnosticRecords : {}
property diagnosticSummaryIndex : 0
property diagnosticBudgetExhausted : false
property diagnosticComplete : true
property diagnosticMissing : 0
property diagnosticErrors : 0
property diagnosticTruncated : 0
property diagnosticProjectionDisagreements : 0
property diagnosticProjectionUnavailable : 0
property diagnosticRecordedNodes : 0
property diagnosticVisitedPaths : {}
property diagnosticDashboardPaths : {}
property diagnosticRefreshPaths : {}
property diagnosticQuitPaths : {}
property diagnosticPath : "1"
property diagnosticPhase : "start"
property diagnosticStartedAt : ""

-- Manual-only, sequential live-subtree observations; never inputs to acceptance.
on resetTraversalDiagnostics()
    set traversalDiagnostics to false
    set diagnosticBytes to 0
    set diagnosticRecords to {}
    set diagnosticSummaryIndex to 0
    set diagnosticBudgetExhausted to false
    set diagnosticComplete to true
    set diagnosticMissing to 0
    set diagnosticErrors to 0
    set diagnosticTruncated to 0
    set diagnosticProjectionDisagreements to 0
    set diagnosticProjectionUnavailable to 0
    set diagnosticRecordedNodes to 0
    set diagnosticVisitedPaths to {}
    set diagnosticDashboardPaths to {}
    set diagnosticRefreshPaths to {}
    set diagnosticQuitPaths to {}
    set diagnosticPath to "1"
    set diagnosticPhase to "start"
    set diagnosticStartedAt to ""
end resetTraversalDiagnostics

on diagnosticByteCount(lineText)
    set encodedText to current application's NSString's stringWithString:lineText
    -- Include newline and a conservative allowance for osascript log framing.
    return (encodedText's lengthOfBytesUsingEncoding:(current application's NSUTF8StringEncoding)) as integer
end diagnosticByteCount

on diagnosticLog(lineText, finalRecord)
    if not traversalDiagnostics then return
    if diagnosticBudgetExhausted and not finalRecord then return
    try
        set completeLine to "CC5-UI-MANUAL-TRAVERSAL " & lineText
        if lineText starts with "CC5-UI-MANUAL-SELECTED " then set completeLine to lineText
        set lineBytes to (my diagnosticByteCount(completeLine)) + 8
        set byteLimit to 24576 -- Reserve 8192 UTF-8 bytes for frontier and final summary.
        if finalRecord then set byteLimit to 32768
        if diagnosticBytes + lineBytes > byteLimit then
            set diagnosticComplete to false
            if not diagnosticBudgetExhausted then
                set diagnosticBudgetExhausted to true
                my diagnosticLog("incomplete=true; truncated=true; frontier=" & diagnosticPath & "; reason=utf8-byte-budget; extra-attribute-reads=stopped", true)
            end if
            return
        end if
        set diagnosticBytes to diagnosticBytes + lineBytes
        set end of diagnosticRecords to {completeLine, lineBytes}
        if lineText starts with "end;" then set diagnosticSummaryIndex to count diagnosticRecords
    on error diagnosticMessage number diagnosticNumber
        set diagnosticComplete to false
        set diagnosticErrors to diagnosticErrors + 1
        set diagnosticBudgetExhausted to true
        -- A formatting/logging failure cannot become an acceptance failure.
        try
            set fallbackLine to "CC5-UI-MANUAL-TRAVERSAL incomplete=true; diagnostic-error=" & diagnosticNumber & "; extra-attribute-reads=stopped"
            set fallbackBytes to (my diagnosticByteCount(fallbackLine)) + 8
            if diagnosticBytes + fallbackBytes <= 32768 then
                set diagnosticBytes to diagnosticBytes + fallbackBytes
                set end of diagnosticRecords to {fallbackLine, fallbackBytes}
            end if
        end try
    end try
end diagnosticLog

on flushTraversalDiagnostics(originalMessage, originalNumber)
    if not traversalDiagnostics or (count diagnosticRecords) is 0 then return
    set emissionReady to false
    try
        if diagnosticSummaryIndex is 0 then my diagnosticEnd("threw", originalNumber)
        -- Manual no-window logs precede all buffering and throw before this stage.
        -- Buffered-stage failures return no stdout and have no original stderr logs.
        set emissionLimit to 65536 - (my diagnosticByteCount(originalMessage)) - (my diagnosticByteCount(originalNumber as text)) - 4096
        if emissionLimit > 32768 then set emissionLimit to 32768
        set markerLine to "CC5-UI-MANUAL-TRAVERSAL incomplete=true; emission-truncated=true; reason=output-budget-or-emission-error; buffered-records-omitted=true"
        set markerBytes to (my diagnosticByteCount(markerLine)) + 8
        if emissionLimit < markerBytes then return
        set markerAttempted to false
        set emissionReady to true
        set summaryBytes to 0
        if diagnosticSummaryIndex > 0 then set summaryBytes to item 2 of item diagnosticSummaryIndex of diagnosticRecords
        -- Reserve both the summary and an explicit omission marker before node details.
        set remainingBytes to emissionLimit - markerBytes - summaryBytes
        if remainingBytes < 0 then
            set markerAttempted to true
            log markerLine
            return
        end if
        set emittedIndexes to {}
        set recordsOmitted to false
        repeat with recordIndex from 1 to count diagnosticRecords
            set recordBytes to item 2 of item recordIndex of diagnosticRecords
            if recordIndex is diagnosticSummaryIndex then
                set end of emittedIndexes to recordIndex as integer
            else if recordBytes <= remainingBytes then
                set end of emittedIndexes to recordIndex as integer
                set remainingBytes to remainingBytes - recordBytes
            else
                set recordsOmitted to true
            end if
        end repeat
        if recordsOmitted then
            set markerAttempted to true
            log markerLine
        end if
        repeat with recordIndex in emittedIndexes
            log (item 1 of item (contents of recordIndex) of diagnosticRecords)
        end repeat
    on error
        if emissionReady and not markerAttempted then
            try
                log markerLine
            end try
        end if
    end try
end flushTraversalDiagnostics

on diagnosticValue(rawValue)
    if rawValue is missing value then
        set diagnosticComplete to false
        set diagnosticMissing to diagnosticMissing + 1
        return "s=missing-value; t=false"
    end if
    set valueText to rawValue as text
    set valueTruncated to (count characters of valueText) > 1024
    if valueTruncated then
        set valueText to text 1 thru 1024 of valueText
        set diagnosticComplete to false
        set diagnosticTruncated to diagnosticTruncated + 1
    end if
    set valueStatus to "present-nonempty"
    if valueText is "" then set valueStatus to "empty"
    set escapedText to current application's NSString's stringWithString:valueText
    set escapedText to escapedText's stringByReplacingOccurrencesOfString:"\\" withString:"\\\\"
    set escapedText to escapedText's stringByReplacingOccurrencesOfString:tab withString:"\\t"
    set escapedText to escapedText's stringByReplacingOccurrencesOfString:return withString:"\\r"
    set escapedText to escapedText's stringByReplacingOccurrencesOfString:linefeed withString:"\\n"
    return "s=" & valueStatus & "; t=" & valueTruncated & "; v=" & (escapedText as text)
end diagnosticValue

on diagnosticField(elementRef, fieldName)
    try
        tell application "System Events"
            if fieldName is "role" then
                set rawField to role of elementRef
            else if fieldName is "name" then
                set rawField to name of elementRef
            else if fieldName is "value" then
                set rawField to value of elementRef
            else
                if not (exists attribute fieldName of elementRef) then
                    set my diagnosticComplete to false
                    set my diagnosticMissing to (my diagnosticMissing) + 1
                    return "s=missing-attribute; t=false"
                end if
                set rawField to value of attribute fieldName of elementRef
            end if
        end tell
        return my diagnosticValue(rawField)
    on error diagnosticMessage number diagnosticNumber
        set diagnosticComplete to false
        set diagnosticErrors to diagnosticErrors + 1
        return "s=read-error; e=" & diagnosticNumber & "; t=false"
    end try
end diagnosticField

on diagnosticActions(elementRef, pathText, depth)
    try
        tell application "System Events" to set advertisedActions to name of actions of elementRef
        if advertisedActions is missing value then
            my diagnosticLog("path=" & pathText & "; depth=" & depth & "; actions={" & my diagnosticValue(missing value) & "}", false)
            return
        end if
        if (class of advertisedActions) is not list then error "Diagnostic actions are non-list" number -1700
        set actionCount to count advertisedActions
        set actionLimit to actionCount
        if actionLimit > 16 then
            set actionLimit to 16
            set diagnosticComplete to false
            set diagnosticTruncated to diagnosticTruncated + 1
        end if
        set actionrow to "path=" & pathText & "; depth=" & depth & "; actions-count=" & actionCount & "; actions-truncated=" & (actionCount > 16)
        repeat with actionIndex from 1 to actionLimit
            if diagnosticBudgetExhausted then exit repeat
            set actionrow to actionrow & "; action[" & actionIndex & "]={" & my diagnosticValue(item actionIndex of advertisedActions) & "}"
        end repeat
        my diagnosticLog(actionrow, false)
    on error diagnosticMessage number diagnosticNumber
        set diagnosticComplete to false
        set diagnosticErrors to diagnosticErrors + 1
        my diagnosticLog("path=" & pathText & "; depth=" & depth & "; actions=read-error; error-number=" & diagnosticNumber, false)
    end try
end diagnosticActions

on diagnosticNode(elementRef, pathText, depth)
    if diagnosticBudgetExhausted then return missing value
    try
        set diagnosticRecordedNodes to diagnosticRecordedNodes + 1
        set noderow to "path=" & pathText & "; depth=" & depth
        repeat with fieldName in {"role", "name", "value", "AXTitle", "AXDescription", "AXEnabled"}
            if diagnosticBudgetExhausted then return missing value
            set noderow to noderow & "; " & (contents of fieldName) & "={" & my diagnosticField(elementRef, contents of fieldName) & "}"
        end repeat
        my diagnosticLog(noderow, false)
        if diagnosticBudgetExhausted then return missing value
        my diagnosticActions(elementRef, pathText, depth)
        if diagnosticBudgetExhausted then return missing value
        tell application "System Events"
            if not (exists attribute "AXChildren" of elementRef) then
                set rawChildrenStatus to "missing-attribute"
                set rawChildren to missing value
            else
                set rawChildren to value of attribute "AXChildren" of elementRef
                set rawChildrenStatus to "missing-value"
            end if
        end tell
        if rawChildren is not missing value then
            if (class of rawChildren) is list then
                set rawCount to count rawChildren
                my diagnosticLog("path=" & pathText & "; depth=" & depth & "; raw-AXChildren=present-list; raw-count=" & rawCount, false)
                return rawCount
            end if
            set rawChildrenStatus to "non-list"
        end if
        set diagnosticComplete to false
        set diagnosticProjectionUnavailable to diagnosticProjectionUnavailable + 1
        my diagnosticLog("path=" & pathText & "; depth=" & depth & "; raw-AXChildren=" & rawChildrenStatus & "; raw-count=unavailable", false)
    on error diagnosticMessage number diagnosticNumber
        set diagnosticComplete to false
        set diagnosticErrors to diagnosticErrors + 1
        set diagnosticProjectionUnavailable to diagnosticProjectionUnavailable + 1
        my diagnosticLog("path=" & pathText & "; depth=" & depth & "; raw-AXChildren=read-error; error-number=" & diagnosticNumber & "; raw-count=unavailable", false)
    end try
    return missing value
end diagnosticNode

on diagnosticChildren(pathText, depth, uiCount, rawCount, roleText, nameText, valueText)
    try
        set comparisonText to "unavailable"
        if rawCount is not missing value then
            set comparisonText to "equal"
            if rawCount is not uiCount then
                set comparisonText to "different"
                set diagnosticComplete to false
                set diagnosticProjectionDisagreements to diagnosticProjectionDisagreements + 1
            end if
        else
            set diagnosticComplete to false
        end if
        my diagnosticLog("path=" & pathText & "; depth=" & depth & "; child-count=" & uiCount & "; UI-elements-count=" & uiCount & "; projection-counts-only=" & comparisonText & "; selector-role={" & my diagnosticValue(roleText) & "}; selector-name={" & my diagnosticValue(nameText) & "}; selector-value={" & my diagnosticValue(valueText) & "}", false)
    on error diagnosticMessage number diagnosticNumber
        set diagnosticComplete to false
        set diagnosticErrors to diagnosticErrors + 1
        my diagnosticLog("path=" & pathText & "; child-count-or-selector-format-error=" & diagnosticNumber, false)
    end try
end diagnosticChildren

on diagnosticPaths(pathList)
    set pathText to ""
    repeat with ordinalPath in pathList
        if pathText is not "" then set pathText to pathText & ","
        set pathText to pathText & (contents of ordinalPath)
        if (count characters of pathText) > 1024 then
            set diagnosticComplete to false
            set diagnosticTruncated to diagnosticTruncated + 1
            return (text 1 thru 1024 of pathText) & "[paths-truncated]"
        end if
    end repeat
    return pathText
end diagnosticPaths

on diagnosticEnd(traversalState, exceptionNumber)
    if not traversalDiagnostics then return
    try
        if traversalState is not "completed" then set diagnosticComplete to false
        set visitedPaths to my diagnosticPaths(diagnosticVisitedPaths)
        set dashboardPaths to my diagnosticPaths(diagnosticDashboardPaths)
        set refreshPaths to my diagnosticPaths(diagnosticRefreshPaths)
        set quitPaths to my diagnosticPaths(diagnosticQuitPaths)
        my diagnosticLog("end; started-at=" & diagnosticStartedAt & "; ended-at=" & (current date as text) & "; traversal=" & traversalState & "; phase=" & diagnosticPhase & "; frontier=" & diagnosticPath & "; exception-number=" & exceptionNumber & "; total-visited=" & nodeCount & "; recorded-nodes=" & diagnosticRecordedNodes & "; visited-paths=" & visitedPaths & "; dashboard-count=" & (count dashboardButtons) & "; dashboard-paths=" & dashboardPaths & "; refresh-count=" & (count refreshButtons) & "; refresh-paths=" & refreshPaths & "; quit-count=" & (count quitButtons) & "; quit-paths=" & quitPaths & "; complete=" & diagnosticComplete & "; missing=" & diagnosticMissing & "; attribute-or-format-errors=" & diagnosticErrors & "; truncated-fields=" & diagnosticTruncated & "; budget-exhausted=" & diagnosticBudgetExhausted & "; raw-count-disagreements=" & diagnosticProjectionDisagreements & "; raw-count-unavailable=" & diagnosticProjectionUnavailable & "; sequential-live-subtree=true; atomic-snapshot=false; physical-visibility-human-action-product-acceptance=not-established; raw-child-reference-correspondence=not-established", true)
    on error diagnosticMessage number diagnosticNumber
        set diagnosticComplete to false
        my diagnosticLog("end; incomplete=true; summary-error=" & diagnosticNumber & "; traversal=" & traversalState & "; original-exception-number=" & exceptionNumber, true)
    end try
end diagnosticEnd

on boundedText(rawValue)
    if rawValue is missing value then return ""
    set textValue to rawValue as text
    if (count characters of textValue) > 1024 then error "Accessibility text exceeds bound"
    set AppleScript's text item delimiters to {tab, return, linefeed}
    set pieces to text items of textValue
    set AppleScript's text item delimiters to " "
    set cleanValue to pieces as text
    set AppleScript's text item delimiters to ""
    return cleanValue
end boundedText

on ownedActivationEvidence(ownedPid)
    set nativeApplication to current application's NSRunningApplication's runningApplicationWithProcessIdentifier:ownedPid
    if nativeApplication is missing value then return "native=unavailable"
    set nativePid to nativeApplication's processIdentifier() as integer
    if nativePid is not ownedPid then error "Native process identifier does not match retained PID"
    set nativePolicy to nativeApplication's activationPolicy() as integer
    set nativeActive to nativeApplication's isActive() as boolean
    set nativeFinishedLaunching to nativeApplication's isFinishedLaunching() as boolean
    return "pid=" & nativePid & "; policy=" & nativePolicy & "; active=" & nativeActive & "; finishedLaunching=" & nativeFinishedLaunching
end ownedActivationEvidence

on elementEvidence(elementRef)
    set evidenceText to ""
    tell application "System Events"
        repeat with attributeName in {"AXRole", "AXSubrole", "AXTitle", "AXDescription", "AXPosition", "AXSize", "AXEnabled", "AXVisible"}
            set attributeText to "unavailable"
            try
                set rawAttribute to value of attribute (contents of attributeName) of elementRef
                if rawAttribute is not missing value then
                    if (contents of attributeName) is in {"AXPosition", "AXSize"} and (class of rawAttribute) is list and (count rawAttribute) is 2 then
                        set attributeText to my boundedText((my boundedText(item 1 of rawAttribute)) & "," & (my boundedText(item 2 of rawAttribute)))
                    else
                        set attributeText to my boundedText(rawAttribute)
                    end if
                end if
            end try
            if evidenceText is not "" then set evidenceText to evidenceText & "; "
            set evidenceText to evidenceText & (contents of attributeName) & "=" & attributeText
        end repeat
    end tell
    if (count characters of evidenceText) > 10000 then error "Accessibility element diagnostic exceeds bound"
    return evidenceText
end elementEvidence

on ownedWindowEvidence(ownedProcess)
    tell application "System Events" to set ownedWindows to windows of ownedProcess
    set windowCount to count ownedWindows
    set evidenceText to "total=" & windowCount & "; truncated=" & (windowCount > 8)
    set inspectedCount to windowCount
    if inspectedCount > 8 then set inspectedCount to 8
    repeat with windowIndex from 1 to inspectedCount
        set evidenceText to evidenceText & "; window[" & windowIndex & "]={" & my elementEvidence(item windowIndex of ownedWindows) & "}"
        if (count characters of evidenceText) > 10000 then
            set truncationSuffix to "; [diagnostic report truncated]"
            set evidenceText to (text 1 thru (10000 - (count characters of truncationSuffix)) of evidenceText) & truncationSuffix
            exit repeat
        end if
    end repeat
    return evidenceText
end ownedWindowEvidence

on ownedRawWindowEvidence(ownedProcess)
    try
        tell application "System Events" to set rawWindows to value of attribute "AXWindows" of ownedProcess
    on error diagnosticMessage number diagnosticNumber
        return "AXWindows=unavailable; read-error=" & diagnosticNumber & "; possible time-limit/read limitation"
    end try
    if rawWindows is missing value then return "AXWindows=unavailable; missing value"
    if (class of rawWindows) is not list then return "AXWindows=unexpected non-list; count=unavailable"
    set windowCount to count rawWindows
    set evidenceText to "AXWindows count=" & windowCount & "; truncated=" & (windowCount > 8) & "; AXVisible=false/unavailable does not establish physical absence"
    set inspectedCount to windowCount
    if inspectedCount > 8 then set inspectedCount to 8
    repeat with windowIndex from 1 to inspectedCount
        try
            set windowEvidence to my elementEvidence(item windowIndex of rawWindows)
        on error diagnosticMessage number diagnosticNumber
            set windowEvidence to "unavailable; read-error=" & diagnosticNumber & "; possible time-limit/read limitation"
        end try
        set evidenceText to evidenceText & "; window[" & windowIndex & "]={" & windowEvidence & "}"
        if (count characters of evidenceText) > 10000 then
            set truncationSuffix to "; [diagnostic report truncated]"
            set evidenceText to (text 1 thru (10000 - (count characters of truncationSuffix)) of evidenceText) & truncationSuffix
            exit repeat
        end if
    end repeat
    return evidenceText
end ownedRawWindowEvidence

on appendHierarchyEvidence(evidenceText)
    if hierarchyReportTruncated then return
    set hierarchyReport to hierarchyReport & evidenceText
    if (count characters of hierarchyReport) > 10000 then
        set truncationSuffix to "; [diagnostic report truncated]"
        set hierarchyReport to (text 1 thru (10000 - (count characters of truncationSuffix)) of hierarchyReport) & truncationSuffix
        set hierarchyReportTruncated to true
    end if
end appendHierarchyEvidence

on walkHierarchyEvidence(elementRef, depth)
    if hierarchyReportTruncated then return
    if hierarchyNodeCount >= 12 then
        my appendHierarchyEvidence("; [node bound truncated]")
        return
    end if
    set hierarchyNodeCount to hierarchyNodeCount + 1
    set diagnosticNodeIndex to hierarchyNodeCount
    set roleText to "unavailable"
    set nameText to "unavailable"
    set valueText to "unavailable"
    set childCountText to "unavailable"
    set diagnosticChildCount to missing value
    tell application "System Events"
        try
            set rawRole to role of elementRef
            if rawRole is not missing value then set roleText to my boundedText(rawRole)
        end try
        try
            set rawName to name of elementRef
            if rawName is not missing value then set nameText to my boundedText(rawName)
        end try
        try
            set rawValue to value of elementRef
            if rawValue is not missing value then set valueText to my boundedText(rawValue)
        end try
        try
            set diagnosticChildCount to count UI elements of elementRef
            set childCountText to diagnosticChildCount as text
        end try
    end tell
    my appendHierarchyEvidence("; node[" & diagnosticNodeIndex & "]={depth=" & depth & "; role=" & roleText & "; name=" & nameText & "; value=" & valueText & "; children=" & childCountText & "}")
    if diagnosticChildCount is missing value or hierarchyReportTruncated then return
    if diagnosticChildCount > 4 then my appendHierarchyEvidence("; [child bound truncated at node " & diagnosticNodeIndex & "]")
    if depth >= 2 then
        if diagnosticChildCount > 0 then my appendHierarchyEvidence("; [depth bound truncated at node " & diagnosticNodeIndex & "]")
        return
    end if
    set inspectedChildCount to diagnosticChildCount
    if inspectedChildCount > 4 then set inspectedChildCount to 4
    repeat with childIndex from 1 to inspectedChildCount
        if hierarchyReportTruncated then exit repeat
        if hierarchyNodeCount >= 12 then
            my appendHierarchyEvidence("; [node bound truncated]")
            exit repeat
        end if
        try
            tell application "System Events" to set diagnosticChild to UI element childIndex of elementRef
            my walkHierarchyEvidence(diagnosticChild, depth + 1)
        on error
            my appendHierarchyEvidence("; child[" & childIndex & "] of node[" & diagnosticNodeIndex & "]=unavailable")
        end try
    end repeat
end walkHierarchyEvidence

on ownedHierarchyEvidence(elementRef)
    set hierarchyNodeCount to 0
    set hierarchyReport to "bounds={nodes=12; depth=2; children=4}"
    set hierarchyReportTruncated to false
    try
        my walkHierarchyEvidence(elementRef, 0)
        return hierarchyReport
    on error
        return "diagnostic failure"
    end try
end ownedHierarchyEvidence

on walkElement(elementRef, depth)
    my walkElementAt(elementRef, depth, "1")
end walkElement

on walkElementAt(elementRef, depth, pathText)
    set nodeCount to nodeCount + 1
    if traversalDiagnostics then
        set diagnosticPath to pathText
        set diagnosticPhase to "visit-bound"
        set end of diagnosticVisitedPaths to pathText
    end if
    if nodeCount > 512 or depth > 12 then error "Accessibility traversal exceeds bound"
    set diagnosticRawCount to missing value
    if traversalDiagnostics then
        set diagnosticPhase to "node-observations"
        set diagnosticRawCount to my diagnosticNode(elementRef, pathText, depth)
        set diagnosticPhase to "original-role-name-value"
    end if
    tell application "System Events"
        set roleText to my boundedText(role of elementRef)
        set rawName to ""
        set rawValue to ""
        try
            set rawName to name of elementRef
        end try
        try
            set rawValue to value of elementRef
        end try
        set nameText to my boundedText(rawName)
        set valueText to my boundedText(rawValue)
        if roleText is "AXButton" then
            if nameText is "" then
                try
                    set nameText to my boundedText(value of attribute "AXTitle" of elementRef)
                end try
            end if
            if nameText is "Open Dashboard" then
                set end of dashboardButtons to elementRef
                if my traversalDiagnostics then set end of my diagnosticDashboardPaths to pathText
            end if
            if nameText is "Refresh" then
                set end of refreshButtons to elementRef
                if my traversalDiagnostics then set end of my diagnosticRefreshPaths to pathText
            end if
            if nameText is "Quit" then
                set end of quitButtons to elementRef
                if my traversalDiagnostics then set end of my diagnosticQuitPaths to pathText
            end if
        end if
        if my traversalDiagnostics then set my diagnosticPhase to "original-UI-elements-enumeration"
        set childElements to UI elements of elementRef
    end tell
    if traversalDiagnostics then
        my diagnosticChildren(pathText, depth, count childElements, diagnosticRawCount, roleText, nameText, valueText)
        set diagnosticPhase to "original-row-and-child-bounds"
    end if
    set end of observedRows to roleText & tab & nameText & tab & valueText
    set end of semanticTexts to nameText
    set end of semanticTexts to valueText
    if (count childElements) + nodeCount > 512 then error "Accessibility child count exceeds bound"
    if depth is 12 and (count childElements) > 0 then error "Accessibility depth exceeds bound"
    set childOrdinal to 0
    repeat with childRef in childElements
        set childOrdinal to childOrdinal + 1
        my walkElementAt(contents of childRef, depth + 1, pathText & "/" & childOrdinal)
    end repeat
end walkElement

on run argv
    my resetTraversalDiagnostics()
    try
        return my runOriginal(argv)
    on error originalMessage number originalNumber
        try
            my flushTraversalDiagnostics(originalMessage, originalNumber)
        end try
        error originalMessage number originalNumber
    end try
end run

on runOriginal(argv)
    if (count argv) is not 2 then error "Expected ownedNativePid and observe|dashboard|observe-open|dashboard-open"
    set ownedPid to item 1 of argv as integer
    set requestedAction to item 2 of argv
    if ownedPid < 1 or requestedAction is not in {"observe", "dashboard", "observe-open", "dashboard-open"} then error "Invalid bounded UI request"
    set traversalDiagnostics to requestedAction is in {"observe-open", "dashboard-open"}
    set nodeCount to 0
    set observedRows to {}
    set semanticTexts to {}
    set dashboardButtons to {}
    set refreshButtons to {}
    set quitButtons to {}
    set appleEventTimeout to 10
    set windowWaitSeconds to 5
    if requestedAction is "observe-open" then
        set appleEventTimeout to 70
        set windowWaitSeconds to 60
    end if
    with timeout of appleEventTimeout seconds
        tell application "System Events"
            set ownedProcesses to application processes whose unix id is ownedPid
            if (count ownedProcesses) is not 1 then error "Exact retained native process is unavailable"
            set ownedProcess to item 1 of ownedProcesses
            if requestedAction is in {"observe", "dashboard"} then
                -- Request frontmost once, then observe the retained process's state within a bounded interval.
                set beforeFrontmost to frontmost of ownedProcess
                log "CC5-UI-DIAGNOSTICS before-frontmost: " & beforeFrontmost
                try
                    log "CC5-UI-DIAGNOSTICS before-native: " & my ownedActivationEvidence(ownedPid)
                on error
                    log "CC5-UI-DIAGNOSTICS before-native: diagnostic failure"
                end try
                set frontmost of ownedProcess to true
                set frontmostDeadline to (current date) + 2
                set afterFrontmost to frontmost of ownedProcess
                repeat while afterFrontmost is not true and (current date) < frontmostDeadline
                    delay 0.1
                    set afterFrontmost to frontmost of ownedProcess
                end repeat
                log "CC5-UI-DIAGNOSTICS after-frontmost: " & afterFrontmost
                try
                    log "CC5-UI-DIAGNOSTICS after-native: " & my ownedActivationEvidence(ownedPid)
                on error
                    log "CC5-UI-DIAGNOSTICS after-native: diagnostic failure"
                end try
                -- System Events frontmost is diagnostic only; owned Menu Bar UI/Dashboard assertions determine acceptance.
                tell ownedProcess
                    -- Select the retained application's extras bar explicitly.
                    set ownedExtrasBar to value of attribute "AXExtrasMenuBar" of ownedProcess
                    if ownedExtrasBar is missing value then error "Owned process extras menu bar is unavailable"
                    if (role of ownedExtrasBar) is not "AXMenuBar" then error "Owned process extras menu bar has an unexpected role"
                    set ownedExtrasItems to menu bar items of ownedExtrasBar
                    if (count ownedExtrasItems) is not 1 then error "Owned extras menu bar item is ambiguous: " & (count ownedExtrasItems)
                    try
                        log "CC5-UI-DIAGNOSTICS before-item: " & my elementEvidence(item 1 of ownedExtrasItems)
                    on error
                        log "CC5-UI-DIAGNOSTICS before-item: diagnostic failure"
                    end try
                    try
                        log "CC5-UI-DIAGNOSTICS before-windows: " & my ownedWindowEvidence(ownedProcess)
                    on error
                        log "CC5-UI-DIAGNOSTICS before-windows: diagnostic failure"
                    end try
                    set visibleWindows to windows whose visible is true
                    if (count visibleWindows) is 0 then
                        set ownedExtrasItem to item 1 of ownedExtrasItems
                        set ownedExtrasActionNames to name of actions of ownedExtrasItem
                        if (count ownedExtrasActionNames) > 16 then error "Owned extras action count exceeds bound"
                        set normalizedExtrasActionNames to {}
                        repeat with actionName in ownedExtrasActionNames
                            set end of normalizedExtrasActionNames to my boundedText(contents of actionName)
                        end repeat
                        set AppleScript's text item delimiters to ", "
                        log "CC5-UI-DIAGNOSTICS extras-actions: " & (normalizedExtrasActionNames as text)
                        set AppleScript's text item delimiters to ""
                        if "AXPress" is not in ownedExtrasActionNames then error "Owned extras item does not advertise AXPress"
                        perform action "AXPress" of ownedExtrasItem
                        log "CC5-UI-DIAGNOSTICS extras-dispatch: AXPress returned"
                    end if
                end tell
            end if
        end tell
        set windowDeadline to (current date) + windowWaitSeconds
        repeat while (current date) < windowDeadline
            if requestedAction is in {"observe-open", "dashboard-open"} then
                tell application "System Events" to set visibleWindows to windows of ownedProcess
            else
                tell application "System Events" to set visibleWindows to windows of ownedProcess whose visible is true
            end if
            if (count visibleWindows) > 1 then error "Owned native window is ambiguous"
            if (count visibleWindows) is 1 then exit repeat
            delay 0.1
        end repeat
        if (count visibleWindows) is not 1 then
            if requestedAction is in {"observe-open", "dashboard-open"} then
                set diagnosticPrefix to "CC5-UI-MANUAL-DIAGNOSTICS "
                set manualDiagnostics to diagnosticPrefix & "mode=" & requestedAction & "; timestamp=" & (current date as text) & "; retained-pid=" & ownedPid
                try
                    set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "system-events-windows: " & my ownedWindowEvidence(ownedProcess)
                on error diagnosticMessage number diagnosticNumber
                    set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "system-events-windows: unavailable; read-error=" & diagnosticNumber & "; possible time-limit/read limitation"
                end try
                try
                    set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "raw-windows: " & my ownedRawWindowEvidence(ownedProcess)
                on error diagnosticMessage number diagnosticNumber
                    set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "raw-windows: unavailable; read-error=" & diagnosticNumber & "; possible time-limit/read limitation"
                end try
                try
                    set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "process-hierarchy: " & my ownedHierarchyEvidence(ownedProcess)
                on error diagnosticMessage number diagnosticNumber
                    set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "process-hierarchy: unavailable; read-error=" & diagnosticNumber & "; possible time-limit/read limitation"
                end try
                try
                    tell application "System Events" to set diagnosticExtrasBar to value of attribute "AXExtrasMenuBar" of ownedProcess
                    if diagnosticExtrasBar is missing value then
                        set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "extras-bar: unavailable; missing value"
                    else
                        tell application "System Events" to set diagnosticExtrasRole to role of diagnosticExtrasBar
                        if diagnosticExtrasRole is missing value then
                            set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "extras-bar: role unavailable"
                        else if diagnosticExtrasRole is not "AXMenuBar" then
                            set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "extras-bar: unexpected role; " & my boundedText(diagnosticExtrasRole)
                        else
                            set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "extras-bar-hierarchy: " & my ownedHierarchyEvidence(diagnosticExtrasBar)
                            tell application "System Events" to set diagnosticExtrasItems to menu bar items of diagnosticExtrasBar
                            set diagnosticExtrasCount to count diagnosticExtrasItems
                            set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "extras-items: count=" & diagnosticExtrasCount
                            if diagnosticExtrasCount is 1 then
                                set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "extras-item-hierarchy: " & my ownedHierarchyEvidence(item 1 of diagnosticExtrasItems)
                            else
                                set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "extras-item: unavailable for inspection; ambiguous/non-unique count=" & diagnosticExtrasCount
                            end if
                        end if
                    end if
                on error diagnosticMessage number diagnosticNumber
                    set manualDiagnostics to manualDiagnostics & linefeed & diagnosticPrefix & "extras: unavailable; read-error=" & diagnosticNumber & "; possible time-limit/read limitation"
                end try
                if (count characters of manualDiagnostics) > 49000 then
                    set truncationSuffix to linefeed & diagnosticPrefix & "[aggregate diagnostic report truncated]"
                    set manualDiagnostics to (text 1 thru (49000 - (count characters of truncationSuffix)) of manualDiagnostics) & truncationSuffix
                end if
                log manualDiagnostics
                error "Owned native window did not appear within " & windowWaitSeconds & " seconds"
            end if
            try
                log "CC5-UI-DIAGNOSTICS after-item: " & my elementEvidence(item 1 of ownedExtrasItems)
            on error
                log "CC5-UI-DIAGNOSTICS after-item: diagnostic failure"
            end try
            try
                log "CC5-UI-DIAGNOSTICS after-windows: " & my ownedWindowEvidence(ownedProcess)
            on error
                log "CC5-UI-DIAGNOSTICS after-windows: diagnostic failure"
            end try
            try
                log "CC5-UI-DIAGNOSTICS after-extras-hierarchy: " & my ownedHierarchyEvidence(item 1 of ownedExtrasItems)
            on error
                log "CC5-UI-DIAGNOSTICS after-extras-hierarchy: diagnostic failure"
            end try
            try
                log "CC5-UI-DIAGNOSTICS after-process-hierarchy: " & my ownedHierarchyEvidence(ownedProcess)
            on error
                log "CC5-UI-DIAGNOSTICS after-process-hierarchy: diagnostic failure"
            end try
            error "Owned native window did not appear within five seconds"
        end if
        if requestedAction is in {"observe-open", "dashboard-open"} then
            set selectedwindow to item 1 of visibleWindows
            try
                set selectedEvidence to "CC5-UI-MANUAL-SELECTED retained-pid=" & ownedPid & "; " & my elementEvidence(selectedwindow)
                if (count characters of selectedEvidence) > 1000 then
                    set selectedSuffix to "; [selected-window evidence truncated]"
                    set selectedEvidence to (text 1 thru (1000 - (count characters of selectedSuffix)) of selectedEvidence) & selectedSuffix
                    set diagnosticComplete to false
                    set diagnosticTruncated to diagnosticTruncated + 1
                end if
                my diagnosticLog(selectedEvidence, false)
            on error diagnosticMessage number diagnosticNumber
                set diagnosticComplete to false
                set diagnosticErrors to diagnosticErrors + 1
                my diagnosticLog("CC5-UI-MANUAL-SELECTED retained-pid=" & ownedPid & "; evidence=unavailable; read-error=" & diagnosticNumber, false)
            end try
            tell application "System Events" to set selectedrole to role of selectedwindow
            if selectedrole is missing value then error "Owned manual native window role is unavailable"
            if selectedrole is not "AXWindow" then error "Owned manual native window has an unexpected role"
        end if
        if traversalDiagnostics then
            try
                set diagnosticStartedAt to current date as text
                my diagnosticLog("start; retained-pid=" & ownedPid & "; mode=" & requestedAction & "; started-at=" & diagnosticStartedAt & "; selected-window-role=" & selectedrole & "; selected-window-role-guard=passed; path=1; sequential-live-subtree=true; equal-child-counts-do-not-prove-reference-correspondence=true; field-keys=s:status,t:truncated,v:value,e:error-number", false)
            on error diagnosticMessage number diagnosticNumber
                set diagnosticComplete to false
                set diagnosticErrors to diagnosticErrors + 1
                my diagnosticLog("start; incomplete=true; diagnostic-error=" & diagnosticNumber, false)
            end try
            try
                my walkElement(item 1 of visibleWindows, 0)
            on error traversalMessage number traversalNumber
                my diagnosticEnd("threw", traversalNumber)
                error traversalMessage number traversalNumber
            end try
            set diagnosticPhase to "before-original-semantic-checks"
            my diagnosticEnd("completed", 0)
        else
            my walkElement(item 1 of visibleWindows, 0)
        end if
        if (count dashboardButtons) is not 1 then error "Open Dashboard button is ambiguous or unavailable"
        if requestedAction is in {"dashboard", "dashboard-open"} then
            tell application "System Events"
                set actualButton to item 1 of dashboardButtons
                set actualName to my boundedText(name of actualButton)
                if actualName is "" then set actualName to my boundedText(value of attribute "AXTitle" of actualButton)
                perform action "AXPress" of actualButton
            end tell
            return "CC5-DASHBOARD-PRESSED" & tab & actualName
        end if
        if (count refreshButtons) is not 1 or (count quitButtons) is not 1 then error "Owned operational controls are ambiguous or unavailable"
        if "Connected" is not in semanticTexts then error "Owned native UI is not Connected"
        repeat with prefixText in {"Control Center:", "Version:", "Uptime:", "Health", "Registered runtimes"}
            set matched to false
            repeat with semanticText in semanticTexts
                if (contents of semanticText) starts with (contents of prefixText) then set matched to true
            end repeat
            if not matched then error "Required native Summary text is unavailable: " & prefixText
        end repeat
        set AppleScript's text item delimiters to linefeed
        set reportText to "CC5-UI-OBSERVED" & linefeed & (observedRows as text)
        set AppleScript's text item delimiters to ""
        if (count characters of reportText) > 60000 then error "Accessibility observation output exceeds bound"
        return reportText
    end timeout
end runOriginal
