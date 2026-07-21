/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

enum OperationalManagementState {
  case AutoManaged, Adopted, Excluded
}

object OperationalComponentManagement {
  def reconcile(
    current: Option[OperationalManagementState],
    developmentsourceavailable: Boolean,
    accepteduseevidence: Boolean,
    exclusionrecordexists: Boolean
  ): Option[OperationalManagementState] = {
    if (exclusionrecordexists) {
      Some(OperationalManagementState.Excluded)
    } else if (developmentsourceavailable) {
      Some(OperationalManagementState.AutoManaged)
    } else if (current.contains(OperationalManagementState.AutoManaged)) {
      Some(OperationalManagementState.AutoManaged)
    } else if (accepteduseevidence || current.contains(OperationalManagementState.Adopted)) {
      Some(OperationalManagementState.Adopted)
    } else {
      None
    }
  }

  def visible(state: OperationalManagementState): Boolean = state != OperationalManagementState.Excluded
}
