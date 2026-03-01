import { Toaster } from 'sonner';
import { RouterProvider } from 'react-router';
import { router } from './routes';

function App() {
  return (
    <>
      <RouterProvider router={router} />
      <Toaster position="bottom-right" richColors closeButton />
    </>
  );
}

export default App;
