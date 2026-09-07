/** "Sam, Alex" <-> ['Sam', 'Alex']. One place, so the CSV column and the chips
 *  can never disagree about what a list of people looks like. */

export function splitPeople(raw: string): string[] {
  return raw
    .split(',')
    .map((name) => name.trim())
    .filter((name) => name !== '')
}

export function joinPeople(people: string[]): string {
  return dedupePeople(people).join(', ')
}

/** Case-insensitive, first spelling wins — so "sam" typed later does not add a
 *  second chip next to "Sam". */
export function dedupePeople(people: string[]): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const name of people) {
    const trimmed = name.trim()
    if (trimmed === '') continue
    const key = trimmed.toLowerCase()
    if (seen.has(key)) continue
    seen.add(key)
    out.push(trimmed)
  }
  return out
}
