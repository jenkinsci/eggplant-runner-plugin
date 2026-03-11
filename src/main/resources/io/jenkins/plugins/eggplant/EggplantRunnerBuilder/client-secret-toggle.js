Behaviour.specify("input[name='_.clientSecret']", "EggplantRunnerBuilder-clientSecret", 0, function(secretInput) {
    // Set initial type to password
    secretInput.type = "password";
    
    // Find the checkbox within the same form entry
    var container = secretInput.closest(".jenkins-form-item") || secretInput.parentElement;
    var checkbox = container.querySelector("input[type='checkbox'][name='showClientSecret']");
    
    if (checkbox) {
        checkbox.checked = false;
        // Direct onclick assignment (overwrites if called multiple times)
        checkbox.onclick = function() {
            if (secretInput.type === "password") {
                secretInput.type = "text";
                checkbox.checked = true;
            } else {
                secretInput.type = "password";
                checkbox.checked = false;
            }
        };
    }
});
