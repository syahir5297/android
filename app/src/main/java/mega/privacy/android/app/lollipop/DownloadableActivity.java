package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.SorterContentActivity;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.FileUtil;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.download.ChatDownloadInfo;
import mega.privacy.android.app.utils.download.DownloadInfo;
import mega.privacy.android.app.utils.download.DownloadLinkInfo;

import static mega.privacy.android.app.utils.LogUtil.*;


public class DownloadableActivity extends SorterContentActivity {

    private DownloadInfo downloadInfo;

    private DownloadLinkInfo linkInfo;

    private ChatDownloadInfo chatDownloadInfo;

    private DatabaseHandler dbH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbH = DatabaseHandler.getDbHandler(this);
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public void setLinkInfo(DownloadLinkInfo linkInfo) {
        this.linkInfo = linkInfo;
    }

    public void setChatDownloadInfo(ChatDownloadInfo chatDownloadInfo) {
        this.chatDownloadInfo = chatDownloadInfo;
    }

    private Uri extractUri(Intent intent, int resultCode) {
        if (intent == null) {
            logWarning("extractUri: result intent is null");
            if (resultCode != Activity.RESULT_OK) {
                Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.download_requires_permission), -1);
            } else {
                Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.no_external_SD_card_detected), -1);
            }
            return null;
        }
        return intent.getData();
    }

    protected void onRequestSDCardWritePermissionFromChat(Intent intent, int resultCode) {
        Uri treeUri = extractUri(intent, resultCode);
        if (treeUri != null) {
            String uriString = treeUri.toString();
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir != null && pickedDir.canWrite()) {
                //save the sd card root uri string
                dbH.setSDCardUri(uriString);
                if (chatDownloadInfo != null) {
                    try {
                        SDCardOperator sdCardOperator = new SDCardOperator(this);
                        ChatController controller = new ChatController(this);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            controller.requestLocalFolder(chatDownloadInfo.getSize(), chatDownloadInfo.getSerializedNodes(), sdCardOperator.getSDCardRoot());
                        } else {
                            controller.download(extractRealPath(treeUri), chatDownloadInfo.getNodeList());
                        }
                    } catch (SDCardOperator.SDCardException e) {
                        e.printStackTrace();
                        logError("Initialize SDCardOperator failed", e);
                    }
                } else {
                    logWarning("Download info is null, cannot download.");
                }
            }
        } else {
            logWarning("tree uri is null!");
        }
    }

    protected void onRequestSDCardWritePermission(Intent intent, int resultCode, @Nullable NodeController nC) {
        Uri treeUri = extractUri(intent, resultCode);
        if (treeUri != null) {
            String uriString = treeUri.toString();
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir != null && pickedDir.canWrite()) {
                //save the sd card root uri string
                dbH.setSDCardUri(uriString);
                try {
                    SDCardOperator sdCardOperator = new SDCardOperator(this);
                    if (nC != null) {
                        if (downloadInfo != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                nC.requestLocalFolder(downloadInfo, sdCardOperator.getSDCardRoot(), null);
                            } else {
                                nC.checkSizeBeforeDownload(extractRealPath(treeUri), null, downloadInfo.getSize(), downloadInfo.getHashes(), downloadInfo.isHighPriority());
                            }
                        } else if (linkInfo != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                nC.intentPickFolder(linkInfo.getNode(), linkInfo.getUrl(), sdCardOperator.getSDCardRoot());
                            } else {
                                nC.downloadTo(linkInfo.getNode(), extractRealPath(treeUri), linkInfo.getUrl());
                            }
                        }
                    } else {
                        NodeController controller = new NodeController(this);
                        //file link
                        if (linkInfo != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                controller.intentPickFolder(linkInfo.getNode(), linkInfo.getUrl(), sdCardOperator.getSDCardRoot());
                            } else {
                                controller.downloadTo(linkInfo.getNode(), extractRealPath(treeUri), linkInfo.getUrl());
                            }
                        } else {
                            //folder link
                            if (this instanceof FolderLinkActivityLollipop) {
                                FolderLinkActivityLollipop activity = (FolderLinkActivityLollipop) this;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    activity.toSelectFolder(downloadInfo.getHashes(), downloadInfo.getSize(), sdCardOperator.getSDCardRoot(), null);
                                } else {
                                    activity.downloadTo(extractRealPath(treeUri), uriString, null, downloadInfo.getSize(), downloadInfo.getHashes());
                                }
                            }
                        }
                    }
                } catch (SDCardOperator.SDCardException e) {
                    e.printStackTrace();
                    logError("Initialize SDCardOperator failed", e);
                }
            }
        } else {
            logWarning("tree uri is null!");
        }
    }

    private String extractRealPath(Uri treeUri) {
        String path = FileUtil.getFullPathFromTreeUri(treeUri, this);
        dbH.setStorageDownloadLocation(path);
        return path;
    }
}
