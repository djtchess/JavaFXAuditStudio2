Produis un rapport Markdown autonome en utilisant l'agent `transparence-openai` et le skill `$openai-transparency-reporter`.

Le rapport doit :

- lister les donnees observees ou potentiellement envoyees a OpenAI ;
- lister les fichiers impliques et la granularite d'exposition ;
- documenter, pour chaque agent du catalogue, une trace de raisonnement observable ;
- distinguer clairement `observe`, `potentiel` et `hors-perimetre` ;
- signaler explicitement les limites de preuve et l'absence d'une chaine de pensee interne brute.
