filepath = '/workspaces/FieldMind/app/src/main/java/chromahub/rhythm/app/features/field/presentation/screens/WeatherDatabaseScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()

# Find the exact ending of WeatherDatabaseScreen before LiveCurrentWeatherCard
idx = content.find('@Composable\nprivate fun LiveCurrentWeatherCard(')
end_section = content[:idx]

lines = end_section.split('\n')
# Last 15 lines
print("=== Last 15 lines before LiveCurrentWeatherCard ===")
for i, line in enumerate(lines[-15:]):
    print(f"{len(lines)-15+i}: {repr(line)}")
