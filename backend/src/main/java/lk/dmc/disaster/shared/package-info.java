/**
 * Shared kernel: API envelope, error handling, acting user, reference data and file storage. Owned
 * by the whole group; change only through a reviewed pull request.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Shared kernel",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package lk.dmc.disaster.shared;
