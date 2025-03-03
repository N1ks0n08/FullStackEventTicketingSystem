import axios from "axios";
import { useState } from "react";

const post_req_endpoint = "https://cs5ztjczz1.execute-api.us-east-1.amazonaws.com/ligmaballs";

function AccountCreationSetup() {
  const [result, updateResult] = useState("");

  async function sendRequest(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    
    const {data} = await axios.post(post_req_endpoint, {
      Name: formData.get('name'),
      City: formData.get('city'),
      Email: formData.get('email')
    }, {headers: {
      'Content-Type': 'application/json',
    }})
    console.log('Server response: ', data)
    updateResult(data)
  }
  
  return (
    <>
      <h2>Please enter the following values: </h2>
      <form onSubmit={sendRequest}>
        <label htmlFor="name">Name: </label>
        <input id="name" name="name" type="text"/>
        <br/>

        <label htmlFor="city">City: </label>
        <input id="city" name="city" type="text"/>
        <br/>

        <label htmlFor="email">Email: </label>
        <input id="email" name="email" type="text"/>
        <br/>
        <button type="submit">Submit</button>
        <hr/>
        <p>Server response: {result}</p>
      </form>
    </>
  );
}

function App() {
  return (
    <>
      <h1> Welcome! </h1>
      <AccountCreationSetup />
    </>
  )
}

export default App
