(() => {
  'use strict'

  document.querySelector('#login-form')
    .addEventListener('submit', async event => {

      event.preventDefault()
      event.stopPropagation()

      let form = event.target;

      if (form.checkValidity()) {


        let formData = new FormData(form);
        let response = await fetch(form.getAttribute("action"), {
          body: formData,
          cache: 'no-cache',
          method: "post",
          mode: 'no-cors',
          redirect: 'follow',
          credentials: 'same-origin'
        })

        let data = await response.json();

        /**
         * Modal
         */
        let resultModalElement = document.getElementById('resultModal');
        const resultModal = new bootstrap.Modal(resultModalElement);
        let title = "Success";
        let modalCloseButton = resultModalElement.querySelector(".modal-footer button")
        if (response.status !== 200) {
          title = "Error";
          modalCloseButton.onclick = function () {
            resultModal.hide();
          };
        } else {
          let subscriptionForm = document.getElementById('registrationForm');
          subscriptionForm.remove();
          modalCloseButton.onclick = function () {
            location.reload();
          };
          modalCloseButton.innerHTML = "Click here to subscribe a new user if needed"
          modalCloseButton.classList.add("btn-link", "text-muted");
          modalCloseButton.classList.remove("btn-secondary");
        }
        let message = data.message;
        if (typeof message === 'undefined') {
          message = "A validation email has been send. <br>Check your mailbox and click on the validation link.<br>If you don't find our email, check your spambox.";
        }

        let resultModalLabel = resultModalElement.querySelector("#resultModalLabel");
        resultModalLabel.innerHTML = title;
        let resultModalBody = resultModalElement.querySelector(".modal-body");
        resultModalBody.innerHTML = message;
        resultModal.show();

      }

      form.classList.add('was-validated')
    }, false)

})();
