package com.example.recipieproject.ui.savedata;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.recipieproject.databinding.FragmentSaveDataBinding;
import com.example.recipieproject.model.Recipe;
import com.example.recipieproject.utils.BitmapUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import android.app.ProgressDialog;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.AlertDialog;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;
import com.example.recipieproject.utils.EmailJS;

public class SaveDataFragment extends Fragment {
    private FragmentSaveDataBinding binding;
    private SaveDataViewModel saveDataViewModel;
    private static final String EMAIL = "brhimakil.1@gmail.com"; 
    private static final String PASSWORD = "wsqm azdg ojvu axqv"; 

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                         ViewGroup container, Bundle savedInstanceState) {
        saveDataViewModel = new ViewModelProvider(this).get(SaveDataViewModel.class);
        binding = FragmentSaveDataBinding.inflate(inflater, container, false);

        saveDataViewModel.initDatabase(requireContext());

        saveDataViewModel.getSavedRecipes().observe(getViewLifecycleOwner(), recipes -> {
            binding.savedRecipesCount.setText(recipes.size() + " recipes saved");
        });

        binding.sendRecipesButton.setOnClickListener(v -> {
            String name = binding.nameInput.getText().toString();
            String email = binding.emailInput.getText().toString();

            if (validateInput(name, email)) {
                sendRecipesByEmail(name, email);
            }
        });

        return binding.getRoot();
    }

    private boolean validateInput(String name, String email) {
        if (name.isEmpty()) {
            binding.nameInput.setError("Name is required");
            return false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.setError("Valid email is required");
            return false;
        }
        return true;
    }

    private void sendRecipesByEmail(String name, String email) {
        List<Recipe> recipes = saveDataViewModel.getSavedRecipes().getValue();
        if (recipes == null || recipes.isEmpty()) {
            Toast.makeText(requireContext(), "No recipes saved yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialAlertDialogBuilder progressDialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sending Email")
            .setMessage("Please wait...")
            .setCancelable(false);
        androidx.appcompat.app.AlertDialog dialog = progressDialog.show();

        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EMAIL, PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject("Your Saved Recipes");

                Multipart multipart = new MimeMultipart();

                BodyPart messageBodyPart = new MimeBodyPart();
                StringBuilder emailBody = new StringBuilder();
                emailBody.append("Hello ").append(name).append(",\n\n");
                emailBody.append("Here are your saved recipes:\n\n");
                messageBodyPart.setText(emailBody.toString());
                multipart.addBodyPart(messageBodyPart);

                for (int i = 0; i < recipes.size(); i++) {
                    Recipe recipe = recipes.get(i);
                    
                    
                    messageBodyPart = new MimeBodyPart();
                    StringBuilder recipeText = new StringBuilder();
                    recipeText.append(i + 1).append(". ").append(recipe.getTitle()).append("\n");
                    recipeText.append("Ingredients:\n").append(recipe.getIngredients()).append("\n\n");
                    recipeText.append("Instructions:\n").append(recipe.getDescription()).append("\n\n");
                    messageBodyPart.setText(recipeText.toString());
                    multipart.addBodyPart(messageBodyPart);

                    if (recipe.getImageData() != null) {
                        Bitmap bitmap = BitmapUtils.byteArrayToBitmap(recipe.getImageData());
                        if (bitmap != null) {
                            File imageFile = new File(requireContext().getCacheDir(), "recipe_" + i + ".jpg");
                            FileOutputStream fos = new FileOutputStream(imageFile);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            fos.close();

                            messageBodyPart = new MimeBodyPart();
                            messageBodyPart.setDataHandler(new DataHandler(new FileDataSource(imageFile)));
                            messageBodyPart.setFileName("recipe_" + i + ".jpg");
                            multipart.addBodyPart(messageBodyPart);
                        }
                    }
                }

                message.setContent(multipart);
                Transport.send(message);

                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Email sent successfully!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Error Sending Email")
                        .setMessage(e.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 