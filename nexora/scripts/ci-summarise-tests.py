import glob, sys, xml.etree.ElementTree as ET

t = f = e = s = 0
rows = []
for path in glob.glob('app/build/test-results/testDebugUnitTest/*.xml'):
    root = ET.parse(path).getroot()
    t += int(root.get('tests', 0)); f += int(root.get('failures', 0))
    e += int(root.get('errors', 0)); s += int(root.get('skipped', 0))
    rows.append((root.get('name', path), root.get('tests', '0'),
                 root.get('failures', '0'), root.get('errors', '0'),
                 root.get('time', '0')))
print(f"**tests={t} failures={f} errors={e} skipped={s}**")
print()
print("| suite | tests | failures | errors | time (s) |")
print("|---|---|---|---|---|")
for name, n, fa, er, tm in sorted(rows):
    print(f"| {name} | {n} | {fa} | {er} | {tm} |")
sys.exit(0)
