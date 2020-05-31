## PersistentBlockMetadataAPI

This is an API to enable saving metadata on blocks using area effect clouds.
One cloud per plugin (using the API) per chunk (containing blocks with metadata from that plugin) is used.

Advantages:
* Saved block data is synced with chunk saving; even in unexpected situations\
  For example, if the server crashes without saving chunks, the metadata will match the state of
  the blocks in the chunk because it only gets saved when the chunk is saved.
  Using an external database could create a mismatch between metadata and block state.
* This solution uses Spigot's built-in apis
* Removing or disabling the plugin storing metadata causes the clouds to fade away - no manual cleanup is needed

Drawbacks:
* Area effect clouds can be killed by commands which wipes saved metadata
* Using entities instead of direct Java objects is significantly less efficient
