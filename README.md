
```xml
            <plugin>
                <groupId>com.yuhaowin.tools</groupId>
                <artifactId>githook-maven-plugin</artifactId>
                <version>1.0.0</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>install</goal>
                        </goals>
                        <configuration>
                            <resource-hooks>
                                <pre-commit>${project.basedir}/src/main/resources/style/pre-commit.sh</pre-commit>
                            </resource-hooks>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

or

```xml
            <plugin>
                <groupId>com.yuhaowin.tools</groupId>
                <artifactId>githook-maven-plugin</artifactId>
                <version>1.0.0</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>install</goal>
                        </goals>
                        <configuration>
                            <hooks>
                                <pre-commit>several bash scripts...</pre-commit>
                            </hooks>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```