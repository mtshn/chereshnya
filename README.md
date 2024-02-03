# CHERESHNYA
Interactive software for quantitative structure-retentions relationships in gas chromatography  (e.g. prediction of Kovats retention indices)

Work in progress, if you have any questions or want to run the software write to e-mail dm.matiushin@mail.ru

**Download here:** https://github.com/mtshn/chereshnya/releases  \
**Windows:** download and unpack release from this site, run **install.bat** (it will install Microsoft Visual C++ redistributable packages), then run **chereshnya.bat**\
Probably (if you have installed many other software before) the **chereshnya.bat** will work even without installation. Java is bundled to release!\
**Linux:** download and unpack release, run "chereshnya.sh". Writing to disk should be permitted. Java must be installed
(sudo apt-get install default-jdk)

Our previous SVEKLA software (https://github.com/mtshn/svekla) is used by this software. SVEKLA is completely included to this release

The instructions how there releases were built are contained in the instruction_how_the_release_was_created.txt file. The procedure of compilation itself is very easy (install JDK, MAVEN and run "mvn package" command), but other binary dependencies  are required. The instructions_how_the_released_were_built.txt file explains were all these file were given.

The releases include many binary dependencies:

1) SVEKLA software https://github.com/mtshn/svekla (MIT license, many other open source binary dependencies (including JDK) are included into SVEKLA binary releases, see corresponding repository) 
2) Portable Python distribution https://github.com/indygreg/python-build-standalone (BSD-3-Clause license)
3) JSME https://jsme-editor.github.io/ (3-clause BSD license)
4) Visual C++ Redistributable packages from Microsoft  (only Windows release)
5) JavaFX, and many other components with permissive and open source licenses were included to JAR file via Maven. See "pom.xml" for more information.
6) RDKit https://www.rdkit.org/ (BSD 3-Clause License) and several other dependencies with permissive licenses were installed via pip into provided Python distribution.


