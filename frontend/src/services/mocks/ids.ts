/** Fixed seed UUIDs, identical to the Flyway seed: 00000000-0000-0000-TTTT-0000000000NN. */
export function seedId(table: number, n: number): string {
  return `00000000-0000-0000-${String(table).padStart(4, '0')}-${String(n).padStart(12, '0')}`
}

export const ID = {
  district: { CMB: seedId(1, 1), GAM: seedId(1, 2), KAL: seedId(1, 3), RAT: seedId(1, 4), KEG: seedId(1, 5) },
  basin: { KELANI: seedId(2, 1), KALU: seedId(2, 2) },
  hazardType: { FLOOD: seedId(3, 1), LANDSLIDE: seedId(3, 2), DROUGHT: seedId(3, 3) },
  org: {
    DMC: seedId(4, 1),
    MOH: seedId(4, 2),
    ARMY: seedId(4, 3),
    NAVY: seedId(4, 4),
    AIR_FORCE: seedId(4, 5),
    POLICE: seedId(4, 6),
    RED_CROSS: seedId(4, 7),
    SARVODAYA: seedId(4, 8),
    DONOR: seedId(4, 9),
  },
  item: { DRY_RATION: seedId(5, 1), WATER: seedId(5, 2), FIRST_AID: seedId(5, 3), HYGIENE: seedId(5, 4) },
  user: {
    DMC_OFFICER: seedId(6, 1),
    DISTRICT_COLOMBO: seedId(6, 2),
    DISTRICT_RATNAPURA: seedId(6, 3),
    CITIZEN_RUWAN: seedId(6, 4),
    VOLUNTEER_THARINDU: seedId(6, 5),
    COORDINATOR: seedId(6, 6),
    RESCUE_ARMY: seedId(6, 7),
    RESCUE_NAVY: seedId(6, 8),
    CITIZEN_PRIYA: seedId(6, 9),
  },
  event: { KELANI: seedId(7, 1), KALU: seedId(7, 2) },
  sensor: (n: number) => seedId(8, n),
  shelter: (n: number) => seedId(9, n),
  team: (n: number) => seedId(10, n),
  stock: (n: number) => seedId(11, n),
}

let counter = 0
/** New record id for mock-created rows. */
export function newId(): string {
  counter += 1
  return crypto.randomUUID?.() ?? `mock-${Date.now()}-${counter}`
}
