# shared-quality

Centralized Checkstyle and quality configuration for Ejada projects.

## Use cases
- Enforce consistent code style across repositories.
- Share suppression rules and Checkstyle configuration.
- Reference in build plugins to apply the same rules everywhere.

## Usage
Reference the configuration in your Maven build:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-checkstyle-plugin</artifactId>
  <configuration>
    <configLocation>shared-quality/checkstyle.xml</configLocation>
    <suppressionsLocation>shared-quality/suppressions.xml</suppressionsLocation>
  </configuration>
</plugin>
```
