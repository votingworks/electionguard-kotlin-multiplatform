# Implementation Notes for spec 1.9

draft 6/21/2023

3.1.4 Election manifest hash

The _original_ manifest file must be kept as part of the election record. Whenever you need the manifest internally,
you must check that The manifest file matches the config.manifestHash, and then reparse it. Do not serialize your own 
version of the manifest, unless you can verify that it matches the original one.