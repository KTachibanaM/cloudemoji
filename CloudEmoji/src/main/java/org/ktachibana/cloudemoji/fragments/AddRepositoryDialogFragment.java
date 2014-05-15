package org.ktachibana.cloudemoji.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.apache.commons.io.FilenameUtils;
import org.ktachibana.cloudemoji.Constants;
import org.ktachibana.cloudemoji.R;
import org.ktachibana.cloudemoji.events.RepositoryAddedEvent;
import org.ktachibana.cloudemoji.models.Repository;

import java.util.List;

import de.greenrobot.event.EventBus;

public class AddRepositoryDialogFragment extends DialogFragment implements Constants {
    EditText urlEditText;
    EditText aliasEditText;

    public AddRepositoryDialogFragment() {
        // Required empty constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_add_repository_dialog, null);
        urlEditText = (EditText) rootView.findViewById(R.id.addRepositoryUrlEditText);
        aliasEditText = (EditText) rootView.findViewById(R.id.addRepositoryAliasEditText);
        builder.setTitle(R.string.add_repo);
        builder.setView(rootView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = urlEditText.getText().toString();
                String extension = FilenameUtils.getExtension(url);
                boolean duplicateUrl = false;

                // Detect duplicate URL
                List<Repository> repositories = Repository.listAll(Repository.class);
                for (Repository repository : repositories) {
                    if (url.equals(repository.getUrl())) {
                        duplicateUrl = true;
                    }
                }
                if (duplicateUrl) {
                    urlEditText.setError(getString(R.string.duplicate_url));
                }
                else
                {
                    // Detect correct file format
                    if (extension.equals("json") || extension.equals("xml")) {
                        String alias = aliasEditText.getText().toString();
                        Repository repository = new Repository(getActivity().getBaseContext(), url, alias);
                        repository.save();
                        EventBus.getDefault().post(new RepositoryAddedEvent(repository));
                        dialog.dismiss();
                    } else {
                        urlEditText.setError(getString(R.string.invalid_repo_format));
                    }
                }
            }
        });
    }
}
