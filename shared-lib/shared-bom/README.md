# shared-bom

Bill of Materials (BOM) providing aligned dependency versions for Ejada services.

## Use cases
- Keep dependency versions consistent across modules.
- Simplify dependency upgrades by centralizing version numbers.
- Provide a single source of approved library versions.

## Usage
Import the BOM in your project's `dependencyManagement` section:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.ejada</groupId>
      <artifactId>shared-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

After importing you can omit versions for dependencies managed by the BOM.
