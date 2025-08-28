# starter-mapstruct

A tiny starter to **standardize MapStruct** usage across services:

- Shared `@MapperConfig` with sensible defaults (`componentModel = spring`, constructor injection, ignore nulls on update, strict unmapped policy).
- Reusable **Common mappers**: UUID/String, time conversions, trimming.
- Simple `PageDto<>` + helper to map `Page<E>` → `PageDto<D>`.

> You still need to enable the **MapStruct annotation processor** in each service using your parent POM:
>
> ```xml
> <plugin>
>   <groupId>org.apache.maven.plugins</groupId>
>   <artifactId>maven-compiler-plugin</artifactId>
>   <configuration>
>     <annotationProcessorPaths>
>       <path>
>         <groupId>org.mapstruct</groupId>
>         <artifactId>mapstruct-processor</artifactId>
>         <version>${mapstruct.version}</version>
>       </path>
>       <path>
>         <groupId>org.projectlombok</groupId>
>         <artifactId>lombok</artifactId>
>         <version>${lombok.version}</version>
>       </path>
>       <path>
>         <groupId>org.projectlombok</groupId>
>         <artifactId>lombok-mapstruct-binding</artifactId>
>         <version>0.2.0</version>
>       </path>
>     </annotationProcessorPaths>
>     <!-- Optional global defaults -->
>     <compilerArgs>
>       <arg>-Amapstruct.defaultComponentModel=spring</arg>
>       <arg>-Amapstruct.defaultInjectionStrategy=constructor</arg>
>       <arg>-Amapstruct.unmappedTargetPolicy=ERROR</arg>
>     </compilerArgs>
>   </configuration>
> </plugin>
> ```
>
> Then, in your mappers:
> ```java
> @Mapper(config = SharedMapstructConfig.class, uses = { CommonMappers.class })
> public interface CustomerMapper {
>   CustomerDto toDto(Customer e);
>   Customer toEntity(CustomerDto d);
>   void update(CustomerDto d, @MappingTarget Customer e); // nulls ignored
> }
> ```

