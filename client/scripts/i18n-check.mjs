import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const messagesDir = path.join(root, "messages");
const sourceDir = path.join(root, "src");

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function flatten(value, prefix = "") {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return [prefix];
  }

  return Object.entries(value).flatMap(([key, child]) =>
    flatten(child, prefix ? `${prefix}.${key}` : key),
  );
}

function hasKey(messages, key) {
  let current = messages;
  for (const part of key.split(".")) {
    if (
      !current ||
      typeof current !== "object" ||
      !Object.prototype.hasOwnProperty.call(current, part)
    ) {
      return false;
    }
    current = current[part];
  }
  return true;
}

function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  return entries.flatMap((entry) => {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return walk(fullPath);
    }
    return /\.(ts|tsx)$/.test(entry.name) ? [fullPath] : [];
  });
}

const en = readJson(path.join(messagesDir, "en.json"));
const vi = readJson(path.join(messagesDir, "vi.json"));
const enKeys = new Set(flatten(en));
const viKeys = new Set(flatten(vi));

const errors = [];

for (const key of enKeys) {
  if (!viKeys.has(key)) {
    errors.push(`Missing vi key: ${key}`);
  }
}

for (const key of viKeys) {
  if (!enKeys.has(key)) {
    errors.push(`Missing en key: ${key}`);
  }
}

for (const filePath of walk(sourceDir)) {
  const relativePath = path.relative(root, filePath);
  const source = fs.readFileSync(filePath, "utf8");
  const namespaces = new Map();

  for (const match of source.matchAll(
    /const\s+(\w+)\s*=\s*useTranslations\(['"]([^'"]+)['"]\)/g,
  )) {
    namespaces.set(match[1], match[2]);
  }

  for (const [localName, namespace] of namespaces) {
    const callPattern = new RegExp(
      `\\b${localName}(?:\\.(?:rich|has))?\\(\\s*['"]([^'"\\\`$]+)['"]`,
      "g",
    );

    for (const match of source.matchAll(callPattern)) {
      const fullKey = `${namespace}.${match[1]}`;
      if (!hasKey(en, fullKey)) {
        errors.push(`${relativePath}: missing key ${fullKey}`);
      }
    }
  }
}

if (errors.length > 0) {
  console.error(errors.join("\n"));
  process.exit(1);
}

console.log("i18n messages and static translation keys are valid.");
