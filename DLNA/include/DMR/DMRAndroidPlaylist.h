#ifndef __DMR_ANDROID_PLAYLIST__
#define __DMR_ANDROID_PLAYLIST__

#include <subRenderer.h>

class playlistRenderer: public subRenderer{
private:
	// always one file open
	int getLineLength(char *pFileBuffer);
	int getListLength(char *pFileBuffer);
public:
	static int NumberOfFiles;
	playlistRenderer();
	~playlistRenderer();

	int preParse(char *filename, unsigned int *NumberOfTracks, char ***MediaTrackURI, SUBRENDERER_TYPE **MediaType, long *TotalTime,char ***ProtocalInfo = NULL);
	int loadMedia(char *filename);
	int Play(char *filename, int speed){return S_OK;};
	void Stop(){};
	int Pause(bool Pause=true){return S_OK;};
};
#endif

